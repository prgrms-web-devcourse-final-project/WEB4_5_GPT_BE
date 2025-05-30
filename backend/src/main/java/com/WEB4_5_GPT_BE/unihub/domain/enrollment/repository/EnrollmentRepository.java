package com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
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
    List<Enrollment> findAllByStudent(Student student);

    /**
     * 주어진 courseId, studentProfileId 조합으로
     * 수강신청 내역을 조회합니다.
     *
     * @param courseId  강좌 ID (course_id)
     * @param studentId 학생 프로필 ID (student_id)
     * @return Optional<Enrollment>
     */
    Optional<Enrollment> findByCourseIdAndStudentId(Long courseId, Long studentId);

    /**
     * 주어진 courseId, studentProfileId 조합으로
     * 수강신청 내역이 존재하는지 확인합니다.
     *
     * @param courseId  강좌 ID (course_id)
     * @param studentId 학생 프로필 ID (student_id)
     * @return true: 존재, false: 존재하지 않음
     */
    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    /**
     * 특정 강좌(courseId)에 등록된 모든 수강신청(Enrollment) 내역을 조회합니다.
     *
     * @param courseId 수강신청 내역을 가져올 강좌 ID
     * @return 해당 강좌에 등록된 모든 Enrollment(수강신청) 리스트
     */
    List<Enrollment> findAllByCourseId(Long courseId);

    /**
     * 특정 강의에 수강신청이 하나라도 존재하는지 확인합니다.
     */
    boolean existsByCourseId(Long courseId);
}
