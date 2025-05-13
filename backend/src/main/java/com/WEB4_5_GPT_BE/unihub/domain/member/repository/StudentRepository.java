package com.WEB4_5_GPT_BE.unihub.domain.member.repository;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    // 학번 + 대학ID로 중복 체크
    boolean existsByStudentCodeAndUniversityId(String studentCode, Long universityId);

    @Query(
            "SELECT sp FROM Student sp "
                    + "JOIN FETCH sp.university "
                    + "JOIN FETCH sp.major "
                    + "WHERE (:universityId IS NULL OR sp.university.id = :universityId) "
                    + "AND (:majorId IS NULL OR sp.major.id = :majorId) "
                    + "AND (:grade IS NULL OR sp.grade = :grade) "
                    + "AND (:semester IS NULL OR sp.semester = :semester)")
    Page<Student> findStudentsWithFilters(
            @Param("universityId") Long universityId,
            @Param("majorId") Long majorId,
            @Param("grade") Integer grade,
            @Param("semester") Integer semester,
            Pageable pageable);
}
