package com.WEB4_5_GPT_BE.unihub.domain.course.dto;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;

import java.util.List;

public record CourseWithFullScheduleResponse(
        Long id,
        String title,
        String major,
        String university,
        String location,
        Integer capacity,
        Integer enrolled,
        Integer credit,
        String professor,
        Integer grade,
        Integer semester,
        String coursePlanAttachment,
        List<CourseScheduleDto> schedule
) {
    public static CourseWithFullScheduleResponse from(Course course) {
        return new CourseWithFullScheduleResponse(
                course.getId(),
                course.getTitle(),
                course.getMajor().getName(),
                course.getMajor().getUniversity().getName(),
                course.getLocation(),
                course.getCapacity(),
                course.getEnrolled(),
                course.getCredit(),
                // TODO
                course.getProfessor() != null ? course.getProfessor().getMember().getName() : null, //
                course.getGrade(),
                course.getSemester(),
                course.getCoursePlanAttachment() != null ? course.getCoursePlanAttachment() : null,
                course.getSchedules().stream()
                        .map(CourseScheduleDto::from)
                        .toList()
        );
    }
}
