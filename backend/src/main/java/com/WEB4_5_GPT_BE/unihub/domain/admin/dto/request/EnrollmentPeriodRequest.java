package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;

public record EnrollmentPeriodRequest(
        @NotNull String universityName,
        @NotNull Integer grade,
        @NotNull String startDateTime,
        @NotNull String endDateTime
) {
}
