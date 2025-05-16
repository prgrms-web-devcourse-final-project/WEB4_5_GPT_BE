package com.WEB4_5_GPT_BE.unihub.domain.course.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.BaseTimeEntity;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

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

  // 조회 성능을 위한 반정규화
  @Column(nullable = false)
  private Long universityId;

  @Column(nullable = false)
  private String location;

  private String professorProfileEmployeeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "`day`", nullable = false, length = 3)
  private DayOfWeek day;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;
}
