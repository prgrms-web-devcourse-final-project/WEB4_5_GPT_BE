package com.WEB4_5_GPT_BE.unihub.domain.course.dto;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;

import java.util.List;

public record TimetableCourseResponse(
        Long courseId,
        String title,
        String professorName,
        String location,
        List<CourseScheduleDto> schedule
) {
    public static TimetableCourseResponse from(Enrollment enrollment) {
        return new TimetableCourseResponse(
                enrollment.getId(),
                enrollment.getCourse().getTitle(),
                enrollment.getCourse().getProfessor().getName(),
                enrollment.getCourse().getLocation(),
                enrollment.getCourse().getSchedules().stream().map(CourseScheduleDto::from).toList()
        );
    }

    public static TimetableCourseResponse from(Course course) {
        return new TimetableCourseResponse(
                course.getId(),
                course.getTitle(),
                course.getProfessor().getName(),
                course.getLocation(),
                course.getSchedules().stream().map(CourseScheduleDto::from).toList()
        );
    }
}
