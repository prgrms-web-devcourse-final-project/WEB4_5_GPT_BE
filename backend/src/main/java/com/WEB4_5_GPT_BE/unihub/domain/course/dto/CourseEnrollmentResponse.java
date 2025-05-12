package com.WEB4_5_GPT_BE.unihub.domain.course.dto;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;

import java.util.List;

public record CourseEnrollmentResponse(
        Long id,
        String title,
        String major,
        String university,
        String location,
        Integer capacity,
        Integer availableSeats,
        Integer credit,
        String professor,
        Integer grade,
        Integer semester,
        String coursePlanAttachment,
        List<CourseScheduleDto> schedule
) {
    public static CourseEnrollmentResponse from(Course course) {
        return new CourseEnrollmentResponse(
                course.getId(),
                course.getTitle(),
                course.getMajor().getName(),
                course.getMajor().getUniversity().getName(),
                course.getLocation(),
                course.getCapacity(),
                course.getCapacity() - course.getEnrolled(),
                course.getCredit(),
                course.getProfessor() != null ? course.getProfessor().getName() : null,
                course.getGrade(),
                course.getSemester(),
                course.getCoursePlanAttachment() != null ? course.getCoursePlanAttachment() : null,
                course.getSchedules().stream()
                        .map(CourseScheduleDto::from)
                        .toList()
        );
    }
}
