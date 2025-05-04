package com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response;

import java.util.List;

public record MyEnrollmentResponse(
        Long enrollmentId,
        Long courseId,
        String majorName,
        String courseTitle,
        String professorName,
        String location,
        List<CourseScheduleResponse> schedule,
        Integer credit,
        Integer grade,
        Integer semester,
        Integer capacity,
        Integer availableSeats
) {
}
