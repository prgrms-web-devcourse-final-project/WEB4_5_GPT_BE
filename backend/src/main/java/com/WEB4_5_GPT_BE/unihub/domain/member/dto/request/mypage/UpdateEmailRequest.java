package com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailRequest(
        @Email @NotBlank String newEmail
) {}