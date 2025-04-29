package com.WEB4_5_GPT_BE.unihub.domain.member.repository;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    @Query(
            "SELECT sp FROM StudentProfile sp "
                    + "JOIN sp.member m "
                    + "WHERE m.role = 'STUDENT' "
                    + "AND (:universityId IS NULL OR sp.university.id = :universityId) "
                    + "AND (:majorId IS NULL OR sp.major.id = :majorId) "
                    + "AND (:grade IS NULL OR sp.grade = :grade) "
                    + "AND (:semester IS NULL OR sp.semester = :semester)")
    Page<StudentProfile> findStudentsWithFilters(
            @Param("universityId") Long universityId,
            @Param("majorId") Long majorId,
            @Param("grade") Integer grade,
            @Param("semester") Integer semester,
            Pageable pageable);
}
