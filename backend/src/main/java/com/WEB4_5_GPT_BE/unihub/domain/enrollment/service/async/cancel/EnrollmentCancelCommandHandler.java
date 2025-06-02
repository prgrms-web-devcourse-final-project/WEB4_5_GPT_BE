package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.cancel;

import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception.EnrollmentNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수강취소 명령 처리 핸들러
 * ‑ EnrollmentCancelCommand를 받아 DB에서 수강신청 내역을 삭제하고,
 * Redis에 저장된 강좌별 수강인원 카운터를 동기화합니다.
 */
@Service
@RequiredArgsConstructor
public class EnrollmentCancelCommandHandler {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final RedissonClient redisson;

    /**
     * 수강취소 처리 메서드
     * <p>
     * - REQUIRES_NEW 트랜잭션으로 독립 커밋/롤백 보장
     * - 기존 수강신청 엔티티 조회 → 삭제 → 수강가능 인원 카운터 감소
     * - 처리 중 예외 발생 시 Redis 카운터를 원복하고 예외 처리
     *
     * @param cmd 수강취소 명령 (studentId, courseId 포함)
     * @throws EnrollmentNotFoundException 수강신청 내역이 없는 경우
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(EnrollmentCancelCommand cmd) {
        Long studentId = cmd.studentId();
        Long courseId = cmd.courseId();

        try {
            // 1) 수강신청 내역 조회. 없으면 EnrollmentNotFoundException 발생
            Enrollment enrollment = enrollmentRepository
                    .findByCourseIdAndStudentId(courseId, studentId)
                    .orElseThrow(EnrollmentNotFoundException::new);

            // 2) 조회된 수강신청 삭제
            enrollmentRepository.delete(enrollment);

            // 3) 강좌 엔티티의 현재 수강인원 감소
            courseRepository.decrementEnrolled(courseId);

        } catch (Exception e) {
            // 예외 발생 시 Redis 강좌별 수강인원 카운터 원복 (증가)
            redisson.getAtomicLong("course:" + courseId + ":enrolled")
                    .incrementAndGet();
            throw e;
        }
    }
}