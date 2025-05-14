package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TimetableCreateRequest(
        @NotNull(message = "연도는 필수 입력 항목입니다.")
        Integer year,

        @NotNull(message = "학기는 필수 입력 항목입니다.")
        @Min(value = 1, message = "학기는 1 또는 2여야 합니다.")
        @Max(value = 2, message = "학기는 1 또는 2여야 합니다.")
        Integer semester

) {}