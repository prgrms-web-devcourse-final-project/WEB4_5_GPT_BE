package com.WEB4_5_GPT_BE.unihub.domain.university.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MajorUpdateRequest(
    @NotBlank(message = "전공 이름은 필수입니다.") @Size(max = 100, message = "전공 이름은 100자를 초과할 수 없습니다.")
        String name) {}
