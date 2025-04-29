package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response;

import java.time.LocalDate;

/**
 * 수강신청 기간 응답 DTO
 */
public record EnrollmentPeriodResponse(
        Long id, String universityName, Integer grade, LocalDate startDate, LocalDate endDate) {
}
