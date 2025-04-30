package com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage;

import lombok.Builder;

import java.util.List;

@Builder
public record ProfessorCourseResponse(
        Long courseId,
        String title,
        String major,
        String location,
        List<CourseScheduleResponse> schedule,  // 강의 시간 여러개
        Integer capacity,
        Integer enrolled,
        Integer credit,
        Integer grade,
        Integer semester,
        String coursePlanAttachment
) {
}
