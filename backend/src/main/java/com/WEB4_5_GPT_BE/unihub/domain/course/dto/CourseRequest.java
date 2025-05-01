package com.WEB4_5_GPT_BE.unihub.domain.course.dto;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CourseRequest(
        @NotEmpty String title,
        @NotEmpty String major,
        @NotEmpty String university,
        @NotEmpty String location,
        @NotEmpty Integer capacity,
        @NotEmpty Integer credit,
        String employeeId,
        @NotEmpty Integer grade,
        @NotEmpty Integer semester,
        String coursePlanAttachment,
        List<CourseScheduleDto> schedule
) {
    public Course toEntity(Major major, Integer enrolled, ProfessorProfile professorProfile) {
        return new Course(
                null,
                title,
                major,
                location,
                capacity,
                enrolled,
                credit,
                professorProfile,
                grade,
                semester,
                coursePlanAttachment
        );
    }
}
