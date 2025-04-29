package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;

public record EnrollmentPeriodRequest(
        @NotNull Long universityId,
        @NotNull Integer grade,
        @NotNull String startDate,
        @NotNull String endDate) {
}
