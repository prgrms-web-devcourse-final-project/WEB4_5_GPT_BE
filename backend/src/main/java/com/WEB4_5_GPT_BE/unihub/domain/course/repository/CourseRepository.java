package com.WEB4_5_GPT_BE.unihub.domain.course.repository;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface CourseRepository extends JpaRepository<Course, Long> {
    // TODO: 조회 성능을 위해 Course 테이블 또는 ProfessorProfile 테이블 비정규화 고려
    /*
    교수명 검색을 위해 ProfessorProfile, Member 테이블 조인
    대학별 강의 필터링을 위해 Major 테이블 조인
     */
    @Query("""
        SELECT c
        FROM Course c
        LEFT JOIN ProfessorProfile p
            ON c.professor = p
        INNER JOIN Member me
            ON p.member = me
        INNER JOIN Major mj
            ON c.major = mj
        WHERE mj.university.id = :univId
            AND c.title LIKE CONCAT('%', :title, '%')
            AND COALESCE(me.name, "") LIKE CONCAT('%', :profName, '%')
    """)
    Page<Course> findWithFilters(
            @Param("univId") Long univId,
            @Param("title") String title,
            @Param("profName") String profName,
            @Param("majorId") Long majorId,
            @Param("grade") Integer grade,
            @Param("semester") Integer semester,
            Pageable pageable
    );

    List<Course> findByProfessorId(Long professorId);
}
