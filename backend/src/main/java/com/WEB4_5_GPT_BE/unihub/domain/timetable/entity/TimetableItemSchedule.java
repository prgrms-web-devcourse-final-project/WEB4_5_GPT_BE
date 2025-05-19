package com.WEB4_5_GPT_BE.unihub.domain.timetable.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.BaseTimeEntity;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "timetable_item_schedule")
public class TimetableItemSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_item_id", nullable = false)
    private TimetableItem timetableItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "`day`", nullable = false, length = 3)
    private DayOfWeek day; // MON, TUE, ...

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}