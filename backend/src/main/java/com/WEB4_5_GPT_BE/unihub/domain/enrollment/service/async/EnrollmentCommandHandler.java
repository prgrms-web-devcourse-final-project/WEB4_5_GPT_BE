package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async;

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

@Service
@RequiredArgsConstructor
public class EnrollmentCommandHandler {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final RedissonClient redisson;

    /**
     * 실제 DB 저장 + 카운터 동기화 로직.
     * REQUIRES_NEW 트랜잭션을 통해 독립 커밋/롤백 보장.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(EnrollmentCommand cmd) {
        Long studentId = cmd.studentId();
        Long courseId = cmd.courseId();

        try {
            var student = studentRepository.findById(studentId)
                    .orElseThrow(StudentProfileNotFoundException::new);

            var course = courseRepository.findById(courseId)
                    .orElseThrow(CourseNotFoundException::new);

            // 수강신청 정보 생성 및 저장
            Enrollment e = Enrollment.builder()
                    .student(student)
                    .course(course)
                    .build();
            enrollmentRepository.save(e);

            // Course enrolled(수강인원)을 증가시킴
            courseRepository.incrementEnrolled(courseId);

        } catch (Exception e) {
            // DB 저장 실패 시 Redis의 enrolled 카운터를 원래대로 감소시킴
            redisson.getAtomicLong("course:" + courseId + ":enrolled")
                    .decrementAndGet();
            throw e;
        }
    }
}