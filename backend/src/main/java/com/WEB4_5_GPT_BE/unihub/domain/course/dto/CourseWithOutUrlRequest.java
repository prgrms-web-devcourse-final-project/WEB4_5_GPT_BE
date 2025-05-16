package com.WEB4_5_GPT_BE.unihub.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "하나의 강의를 나타내는 DTO.")
public record CourseWithOutUrlRequest(
        @Schema(description = "강의 제목")
        @NotEmpty String title,
        @Schema(description = "강의 전공")
        @NotEmpty String major,
        @Schema(description = "소속 대학")
        @NotEmpty String university,
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
        @Schema(description = "강의 스케줄")
        List<CourseScheduleDto> schedule
) {

    /**
     * 새로운 coursePlanAttachment를 추가한 새 CourseRequest 를 만듭니다.
     */
    public CourseRequest withCoursePlanAttachment(String newAttachment) {
        return new CourseRequest(
                this.title,
                this.major,
                this.university,
                this.location,
                this.capacity,
                this.credit,
                this.employeeId,
                this.grade,
                this.semester,
                newAttachment,
                this.schedule
        );
    }
}
