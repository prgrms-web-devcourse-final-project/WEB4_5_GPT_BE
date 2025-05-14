package com.WEB4_5_GPT_BE.unihub.domain.timetable.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "timetable_item_schedule")
public class TimetableItemSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_item_id", nullable = false)
    private TimetableItem timetableItem;

    @Enumerated(EnumType.STRING)
    @Column(length = 3, nullable = false)
    private DayOfWeek day; // MON, TUE, ...

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}