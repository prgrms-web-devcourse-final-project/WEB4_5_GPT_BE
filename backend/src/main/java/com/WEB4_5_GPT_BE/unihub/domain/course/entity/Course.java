package com.WEB4_5_GPT_BE.unihub.domain.course.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.BaseTimeEntity;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course")
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(nullable = false)
    private Integer capacity; // 수강 가능 최대 인원

    @Column(nullable = false)
    private Integer enrolled; // 현재 수강 인원

    @Column(nullable = false)
    private Integer credit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private Professor professor; // null 허용 시 강의 미배정 상태

    @Column(nullable = false)
    private Integer grade; // 대상 학년

    @Column(nullable = false)
    private Integer semester;

    @Column(length = 255)
    private String coursePlanAttachment; // URL

    /* 시간표 */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<CourseSchedule> schedules = new ArrayList<>();

    /**
     * 강의의 스케줄을 간략화한 문자열을 반환한다.
     *
     * @return {@code [DDDHH-HH]...} 형식의 문자열
     */
    public String scheduleToString() {
        StringBuilder scheduleString = new StringBuilder();
        for (CourseSchedule s : schedules) {
            scheduleString.append(s.getDay()).append(s.getStartTime().getHour()).append("-").append(s.getEndTime().getHour());
        }
        return scheduleString.toString();
    }

    /**
     * 현재 수강 신청 가능한 인원 수를 반환한다.
     *
     * @return 수강 최대 인원 - 현재 수강 인원
     */
    public int getAvailableSeats() {
        return capacity - enrolled;
    }

    /**
     * 현재 강좌 수강 인원을 감소시킨다. (수강취소 완료)
     * 현재 수강 인원이 0보다 큰 경우에만 작동한다.
     */
    public void decrementEnrolled() {
        if (enrolled > 0) {
            enrolled--;
        }
    }

    /**
     * 현재 강좌 수강 인원을 증가시킨다. (수강신청 완료)
     * 현재 수강 인원이 최대 수강 가능 인원보다 작은 경우에만 작동한다.
     */
    public void incrementEnrolled() {
        if (enrolled < capacity) {
            enrolled++;
        }
    }
}
