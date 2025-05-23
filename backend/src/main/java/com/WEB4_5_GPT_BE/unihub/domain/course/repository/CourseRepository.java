package com.WEB4_5_GPT_BE.unihub.domain.course.repository;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface CourseRepository extends JpaRepository<Course, Long> {
    // TODO: 조회 성능을 위해 Course 테이블 또는 ProfessorProfile 테이블 비정규화 고려
    /*
    교수명 검색을 위해 Member 테이블 조인
    대학별 강의 필터링을 위해 Major 테이블 조인
     */
    @Query("""
                SELECT c
                FROM Course c
                LEFT JOIN Professor p
                    ON c.professor = p
                INNER JOIN Major mj
                    ON c.major = mj
                WHERE mj.university.id = :univId
                    AND c.title LIKE CONCAT('%', :title, '%')
                    AND COALESCE(p.name, "") LIKE CONCAT('%', :profName, '%')
                    AND (:majorId IS NULL OR mj.id = :majorId)
                    AND (:grade IS NULL OR c.grade = :grade)
                    AND (:semester IS NULL OR c.semester = :semester)
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

    /**
     * course.enrolled 컬럼을 1 증가시킵니다.
     * (동시성 처리를 DB 레벨에서 atomic 하게 해 줍니다.)
     *
     * @param courseId 증가시킬 Course의 ID
     */
    @Modifying
    @Query("UPDATE Course c SET c.enrolled = c.enrolled + 1 WHERE c.id = :courseId")
    void incrementEnrolled(@Param("courseId") Long courseId);

    /**
     * course.enrolled 컬럼을 1 감소시킵니다.
     * (동시성 처리를 DB 레벨에서 atomic 하게 해 줍니다.)
     *
     * @param courseId 감소시킬 Course의 ID
     */
    @Modifying
    @Query("UPDATE Course c SET c.enrolled = c.enrolled - 1 WHERE c.id = :courseId")
    void decrementEnrolled(@Param("courseId") Long courseId);


}
