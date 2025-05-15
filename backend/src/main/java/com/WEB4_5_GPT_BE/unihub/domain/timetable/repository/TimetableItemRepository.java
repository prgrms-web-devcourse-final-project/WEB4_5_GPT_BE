package com.WEB4_5_GPT_BE.unihub.domain.timetable.repository;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.TimetableItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimetableItemRepository extends JpaRepository<TimetableItem, Long> {
    
    @Query("SELECT ti FROM TimetableItem ti " +
            "LEFT JOIN FETCH ti.schedules " +
            "WHERE ti.timetable.id = :timetableId")
    List<TimetableItem> findWithSchedulesByTimetableId(@Param("timetableId") Long timetableId);
}