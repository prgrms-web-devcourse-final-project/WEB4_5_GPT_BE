package com.WEB4_5_GPT_BE.unihub.domain.timetable.repository;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.TimetableItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TimetableItemRepository extends JpaRepository<TimetableItem, Long> {

    @Query("SELECT ti FROM TimetableItem ti " +
            "LEFT JOIN FETCH ti.schedules " +
            "WHERE ti.timetable.id = :timetableId")
    List<TimetableItem> findWithSchedulesByTimetableId(@Param("timetableId") Long timetableId);

    // 시간표 항목 상세 조회
    @Query("SELECT ti FROM TimetableItem ti " +
            "LEFT JOIN FETCH ti.schedules " +
            "WHERE ti.id = :itemId")
    Optional<TimetableItem> findWithSchedulesById(@Param("itemId") Long itemId);

    // 특정 시간표에 등록된 강의 조회
    Optional<TimetableItem> findByTimetableIdAndCourse(Long timetableId, Course course);

    // 특정 멤버가 시간표 항목 소유 여부 확인
    @Query("SELECT COUNT(ti) > 0 FROM TimetableItem ti " +
            "WHERE ti.id = :itemId AND ti.timetable.member.id = :memberId")
    boolean existsByIdAndMemberId(@Param("itemId") Long itemId, @Param("memberId") Long memberId);
}