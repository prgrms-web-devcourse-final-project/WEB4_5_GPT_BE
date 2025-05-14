package com.WEB4_5_GPT_BE.unihub.domain.course.dto;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;

import java.util.List;

public record MyEnrollmentListForTimetableResponse(
        Long courseId,
        String title,
        String professorName,
        String location,
        List<CourseScheduleDto> schedule
) {
    public static MyEnrollmentListForTimetableResponse from(Enrollment enrollment) {
        return new MyEnrollmentListForTimetableResponse(
                enrollment.getId(),
                enrollment.getCourse().getTitle(),
                enrollment.getCourse().getProfessor().getName(),
                enrollment.getCourse().getLocation(),
                enrollment.getCourse().getSchedules().stream().map(CourseScheduleDto::from).toList()
        );
    }
}
