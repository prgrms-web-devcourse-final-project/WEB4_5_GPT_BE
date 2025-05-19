package com.WEB4_5_GPT_BE.unihub.domain.course.repository;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {

    @Query("""
                SELECT cs
                FROM CourseSchedule cs
                WHERE cs.universityId = :universityId
                    AND cs.professorProfileEmployeeId = :professorEmpId
            """)
    List<CourseSchedule> findByUniversityIdAndProfessorEmployeeId(
            @Param("universityId") Long universityId,
            @Param("professorEmpId") String professorEmpId
            );

    @Query("""
                SELECT cs
                FROM CourseSchedule cs
                WHERE cs.universityId = :universityId
                    AND cs.professorProfileEmployeeId = :professorEmpId
                    AND cs.course.id != :courseId
            """)
    List<CourseSchedule> findByUniversityIdAndProfessorEmployeeIdExcludingCourse(
            @Param("universityId") Long universityId,
            @Param("professorEmpId") String professorEmpId,
            @Param("courseId") Long courseId
    );

    @Query("""
                SELECT cs
                FROM CourseSchedule cs
                WHERE cs.universityId = :universityId
                    AND cs.location = :location
            """)
    List<CourseSchedule> findByUniversityIdAndLocation(
            @Param("universityId") Long universityId,
            @Param("location") String location);

    @Query("""
                SELECT cs
                FROM CourseSchedule cs
                WHERE cs.universityId = :universityId
                    AND cs.location = :location
                    AND cs.course.id != :courseId
            """)
    List<CourseSchedule> findByUniversityIdAndLocationExcludingCourse(
            @Param("universityId") Long universityId,
            @Param("location") String location,
            @Param("courseId") Long courseId);
}
