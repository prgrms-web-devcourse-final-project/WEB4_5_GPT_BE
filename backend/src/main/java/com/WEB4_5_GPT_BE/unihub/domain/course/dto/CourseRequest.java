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
        Course res = new Course(
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
        this.schedule.forEach(
                csd -> res.getSchedules().add(csd.toEntity(res, major.getUniversity().getId())));
        return res;
    }

    public static CourseRequest from(Course course) {
        return new CourseRequest(
                course.getTitle(),
                course.getMajor().getName(),
                course.getMajor().getUniversity().getName(),
                course.getLocation(),
                course.getCapacity(),
                course.getCredit(),
                course.getProfessor() != null ? course.getProfessor().getEmployeeId() : null,
                course.getGrade(),
                course.getSemester(),
                course.getCoursePlanAttachment(),
                course.getSchedules().stream().map(CourseScheduleDto::from).toList()
        );
    }
}
