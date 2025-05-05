package com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    /**
     * 특정 학생(StudentProfile)에 등록된 모든 수강신청(Enrollment) 내역을 조회합니다.
     *
     * @param student 수강신청 내역을 가져올 학생의 프로필
     * @return 해당 학생이 신청한 모든 Enrollment(수강신청) 리스트
     */
    List<Enrollment> findAllByStudent(StudentProfile student);

    /**
     * 주어진 courseId, studentProfileId 조합으로
     * 수강신청 내역을 조회합니다.
     *
     * @param courseId  강좌 ID (course_id)
     * @param studentId 학생 프로필 ID (student_id)
     * @return Optional<Enrollment>
     */
    Optional<Enrollment> findByCourseIdAndStudentId(Long courseId, Long studentId);
}
