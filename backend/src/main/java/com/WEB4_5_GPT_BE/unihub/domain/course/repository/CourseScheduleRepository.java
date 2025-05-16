package com.WEB4_5_GPT_BE.unihub.domain.course.repository;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;

public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {

    // TODO: 각 일에 대해 쿼리를 하나씩 보내기보다 강의에 해당하는 CourseSchedule을 목록으로 받아와 서비스에서 검증하도록 수정을 검토 할 것.
    @Query("""
                SELECT COUNT(*) > 0
                FROM CourseSchedule cs
                WHERE cs.professorProfileEmployeeId = :profId
                    AND cs.day = :dayOfWeek
                    AND ((:pStartTime <= cs.startTime AND cs.startTime < :pEndTime)
                    OR (:pStartTime < cs.endTime AND cs.endTime <= :pEndTime))
            """)
    Boolean existsByProfEmpIdAndDayOfWeek(
            @Param("profId") String profId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("pStartTime") LocalTime pStartTime,
            @Param("pEndTime") LocalTime pEndTime);

    @Query("""
                SELECT COUNT(*) > 0
                FROM CourseSchedule cs
                WHERE cs.professorProfileEmployeeId = :profId
                    AND cs.course.id != :courseId
                    AND cs.day = :dayOfWeek
                    AND ((:pStartTime <= cs.startTime AND cs.startTime < :pEndTime)
                    OR (:pStartTime < cs.endTime AND cs.endTime <= :pEndTime))
            """)
    Boolean existsByProfEmpIdAndDayOfWeekExcludingCourse(
            @Param("profId") String profId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("pStartTime") LocalTime pStartTime,
            @Param("pEndTime") LocalTime pEndTime,
            @Param("courseId") Long courseId
    );

    // TODO: 위와 같음.
    @Query("""
                SELECT COUNT(*) > 0
                FROM CourseSchedule cs
                WHERE cs.universityId = :univId
                    AND cs.location = :location
                    AND cs.day = :dayOfWeek
                    AND ((:pStartTime <= cs.startTime AND cs.startTime < :pEndTime)
                    OR (:pStartTime < cs.endTime AND cs.endTime <= :pEndTime))
            """)
    Boolean existsByUnivIdAndLocationAndDayOfWeek(
            @Param("univId") Long univId,
            @Param("location") String location,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("pStartTime") LocalTime pStartTime,
            @Param("pEndTime") LocalTime pEndTime);

    @Query("""
                SELECT COUNT(*) > 0
                FROM CourseSchedule cs
                WHERE cs.universityId = :univId
                    AND cs.location = :location
                    AND cs.course.id != :courseId
                    AND cs.day = :dayOfWeek
                    AND ((:pStartTime <= cs.startTime AND cs.startTime < :pEndTime)
                    OR (:pStartTime < cs.endTime AND cs.endTime <= :pEndTime))
            """)
    Boolean existsByUnivIdAndLocationAndDayOfWeekExcludingCourse(
            @Param("univId") Long univId,
            @Param("location") String location,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("pStartTime") LocalTime pStartTime,
            @Param("pEndTime") LocalTime pEndTime,
            @Param("courseId") Long courseId);
}
