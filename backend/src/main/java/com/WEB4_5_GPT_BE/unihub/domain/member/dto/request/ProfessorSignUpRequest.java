package com.WEB4_5_GPT_BE.unihub.domain.member.dto.request;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfessorSignUpRequest(
    @NotBlank(message = "필수 입력 값이 누락되었습니다") @Email(message = "필수 입력 값이 누락되었습니다")  String email,
    @NotBlank(message = "필수 입력 값이 누락되었습니다") String password,
    @NotBlank(message = "필수 입력 값이 누락되었습니다") String name,
    @NotBlank(message = "필수 입력 값이 누락되었습니다") String employeeId,
    @NotNull(message = "필수 입력 값이 누락되었습니다") Long universityId,
    @NotNull(message = "필수 입력 값이 누락되었습니다") Long majorId,
    @Schema(description = "항상 PROFESSOR입니다. 서버에서 자동 설정되며, 요청 시 생략해도 무방합니다.", allowableValues = {"PROFESSOR"})
    Role role) {}