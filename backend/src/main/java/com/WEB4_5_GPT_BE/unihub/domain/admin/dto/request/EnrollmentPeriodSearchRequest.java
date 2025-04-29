package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request;

/**
 * 수강신청 기간 검색 조건
 */
public record EnrollmentPeriodSearchRequest(
        String universityName,
        String startDateFrom,
        String startDateTo,
        String endDateFrom,
        String endDateTo
) {
}
