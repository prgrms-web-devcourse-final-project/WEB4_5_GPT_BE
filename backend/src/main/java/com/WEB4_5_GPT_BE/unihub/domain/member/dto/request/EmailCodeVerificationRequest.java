package com.WEB4_5_GPT_BE.unihub.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailCodeVerificationRequest(
    @Email @NotBlank String email, @NotBlank String emailCode) {}
