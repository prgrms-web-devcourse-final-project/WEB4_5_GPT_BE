package com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response;

public record CourseScheduleResponse(
        String day,
        String startTime,
        String endTime
) {
}