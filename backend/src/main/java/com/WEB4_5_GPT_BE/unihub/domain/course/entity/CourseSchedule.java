package com.WEB4_5_GPT_BE.unihub.domain.course.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.BaseTimeEntity;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_schedule")
public class CourseSchedule extends BaseTimeEntity {

  @Id @GeneratedValue private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 3)
  private DayOfWeek day;

  @Column(name = "start_time", nullable = false)
  private LocalTime start_time;

  @Column(name = "end_time", nullable = false)
  private LocalTime end_time;
}
