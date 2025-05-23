package com.WEB4_5_GPT_BE.unihub.domain.course.dto;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "하나의 강의를 나타내는 DTO.")
public record CourseRequest(
        @Schema(description = "강의 제목")
        @NotEmpty String title,
        @Schema(description = "강의 전공")
        @NotEmpty String major,
        @Schema(description = "소속 대학")
        String university,
        @Schema(description = "강의장")
        @NotEmpty String location,
        @Schema(description = "수강 정원")
        @NotEmpty Integer capacity,
        @Schema(description = "단위 학점")
        @NotEmpty Integer credit,
        @Schema(description = "강사/교수 사번")
        String employeeId,
        @Schema(description = "대상 학년")
        @NotEmpty Integer grade,
        @Schema(description = "제공 학기")
        @NotEmpty Integer semester,
        @Schema(description = "강의계획서 파일 경로")
        String coursePlanAttachment,
        @Schema(description = "강의 스케줄")
        List<CourseScheduleDto> schedule
) {
    public Course toEntity(Major major, Integer enrolled, Professor professorProfile) {
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
