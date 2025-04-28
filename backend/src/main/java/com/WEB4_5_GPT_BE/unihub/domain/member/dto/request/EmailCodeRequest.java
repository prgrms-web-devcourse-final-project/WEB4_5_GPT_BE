package com.WEB4_5_GPT_BE.unihub.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailCodeRequest(
        @Email @NotBlank String email
) {
}
