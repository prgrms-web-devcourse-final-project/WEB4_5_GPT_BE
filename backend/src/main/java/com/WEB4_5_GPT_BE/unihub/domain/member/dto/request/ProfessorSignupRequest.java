package com.WEB4_5_GPT_BE.unihub.domain.member.dto.request;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfessorSignupRequest(
    @Email @NotBlank String email,
    @NotBlank String password,
    @NotBlank String name,
    @NotBlank String employeeId,
    @NotNull Long universityId,
    @NotNull Long majorId,
    @NotNull Role role) {}
