package com.WEB4_5_GPT_BE.unihub.domain.member.dto.request;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudentSignUpRequest(
    @Email @NotBlank String email,
    @NotBlank String password,
    @NotBlank String name,
    @NotBlank String studentCode,
    @NotNull Long universityId,
    @NotNull Long majorId,
    @NotNull Integer grade,
    @NotNull Integer semester,
    @NotNull Role role) {}
