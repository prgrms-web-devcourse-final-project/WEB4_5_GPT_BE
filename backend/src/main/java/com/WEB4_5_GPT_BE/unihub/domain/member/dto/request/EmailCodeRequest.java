package com.WEB4_5_GPT_BE.unihub.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailCodeRequest(@NotBlank @Email(message = "이메일 형식이 잘못되었습니다.") String email) {}
