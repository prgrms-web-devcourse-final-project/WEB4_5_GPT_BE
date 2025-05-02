package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EnrollmentPeriodRequest(
        @NotNull Long universityId,

        @NotNull
        @Min(value = 0, message = "연도는 0보다 크거나 같아야 합니다")
        @Max(value = 99, message = "연도는 2자리(0-99)로 입력해야 합니다")
        Integer year,        // 학년도 2자리 (예: 25)
        @Min(value = 1, message = "학기는 1 이상이어야 합니다")
        @Max(value = 2, message = "학기는 2 이하여야 합니다")
        @NotNull Integer grade,
        @NotNull
        @Min(value = 1, message = "학기는 1 이상이어야 합니다")
        @Max(value = 2, message = "학기는 2 이하여야 합니다")
        Integer semester,    // 학기 (1-2)
        @NotNull String startDate,
        @NotNull String endDate) {
}
