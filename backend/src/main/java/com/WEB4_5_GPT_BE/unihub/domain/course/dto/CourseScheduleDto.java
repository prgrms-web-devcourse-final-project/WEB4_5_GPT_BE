package com.WEB4_5_GPT_BE.unihub.domain.course.dto;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalTime;

public record CourseScheduleDto(
        @NotEmpty DayOfWeek day,
        @NotEmpty String startTime,
        @NotEmpty String endTime
) {
    public static CourseScheduleDto from(CourseSchedule courseSchedule) {
        return new CourseScheduleDto(
                courseSchedule.getDay(),
                courseSchedule.getStartTime().toString(),
                courseSchedule.getEndTime().toString()
        );
    }

    public CourseSchedule toEntity(Course course, Long universityId) {
        return new CourseSchedule(
                null,
                course,
                universityId,
                course.getLocation(),
                course.getProfessor() != null ? course.getProfessor().getEmployeeId() : null,
                day,
                LocalTime.parse(startTime),
                LocalTime.parse(endTime)
        );
    }
}
