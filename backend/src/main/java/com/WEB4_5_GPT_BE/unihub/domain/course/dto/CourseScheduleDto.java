package com.WEB4_5_GPT_BE.unihub.domain.course.dto;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalTime;

@Schema(description = "강의의 스케줄 단위를 나타내는 DTO.")
public record CourseScheduleDto(
        @Schema(description = "수업 요일")
        @NotEmpty DayOfWeek day,
        @Schema(description = "수업 시작 시각(`H:mm` 또는 `H:mm:ss`)")
        @NotEmpty String startTime,
        @Schema(description = "수업 종료 시각(`H:mm` 또는 `H:mm:ss`)")
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
