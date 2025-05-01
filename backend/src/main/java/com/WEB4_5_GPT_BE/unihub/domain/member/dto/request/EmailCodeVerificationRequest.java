package com.WEB4_5_GPT_BE.unihub.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailCodeVerificationRequest(
    @NotBlank(message = "필수 입력 값이 누락되었습니다") @Email(message = "이메일 형식이 잘못되었습니다.") String email, @NotBlank(message = "필수 입력 값이 누락되었습니다") String emailCode) {}
