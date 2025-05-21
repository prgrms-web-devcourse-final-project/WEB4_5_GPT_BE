package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.enroll;

import com.WEB4_5_GPT_BE.unihub.domain.course.exception.CourseNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage.StudentProfileNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수강신청 명령 처리 핸들러
 * ‑ EnrollmentCommand를 받아 실제 DB에 저장하고,
 * Redis에 저장된 강좌별 수강인원 카운터를 동기화합니다.
 */
@Service
@RequiredArgsConstructor
public class EnrollmentCommandHandler {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    // Redis 연동용 Redisson 클라이언트 (수강인원 카운터 조작)
    private final RedissonClient redisson;

    /**
     * 수강신청 처리 메서드
     *
     * - REQUIRES_NEW 트랜잭션으로 독립 커밋/롤백 보장
     * - 학생/강좌 조회 → 수강신청 기록 저장 → 수강인원 카운터 증가
     * - 처리 중 예외 발생 시 Redis 카운터를 원복하고 예외 던짐
     *
     * @param cmd 수강신청 명령 (studentId, courseId 포함)
     * @throws StudentProfileNotFoundException 학생 정보가 없는 경우
     * @throws CourseNotFoundException 강좌 정보가 없는 경우
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(EnrollmentCommand cmd) {
        Long studentId = cmd.studentId();
        Long courseId = cmd.courseId();
        String flagKey = "enroll:queued:" + studentId + ":" + courseId;

        try {
            // 1) 학생 조회, 없으면 예외
            var student = studentRepository.findById(studentId)
                    .orElseThrow(StudentProfileNotFoundException::new);

            // 2) 강좌 조회, 없으면 예외
            var course = courseRepository.findById(courseId)
                    .orElseThrow(CourseNotFoundException::new);

            // 3) 수강신청 엔티티 생성 및 저장
            Enrollment enrollment = Enrollment.builder()
                    .student(student)
                    .course(course)
                    .build();
            enrollmentRepository.save(enrollment);

            // 4) DB에 강좌 수강인원 카운터 증가
            courseRepository.incrementEnrolled(courseId);

        } catch (Exception e) {
            // 예외 발생 시 Redis 카운터 원복 (감소)
            redisson.getAtomicLong("course:" + courseId + ":enrolled")
                    .decrementAndGet();
            throw e;
        } finally {
            redisson.getBucket(flagKey).delete(); // 큐가 처리되고 나면 flag 해제
        }
    }
}