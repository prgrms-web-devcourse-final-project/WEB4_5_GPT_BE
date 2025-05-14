package com.WEB4_5_GPT_BE.unihub.domain.timetable.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.BaseTimeEntity;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "timetable_item")
public class TimetableItem extends BaseTimeEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    private Timetable timetable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id") // 강의가 아닌 경우 NULL
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(length = 50)
    private String professorName; // 수동 등록용

    @Column(length = 20)
    private String color; // 프론트에서 표시 색상

    @Column(length = 100)
    private String location;

    @Column(length = 300)
    private String memo;

    @OneToMany(mappedBy = "timetableItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimetableItemSchedule> schedule = new ArrayList<>();
}