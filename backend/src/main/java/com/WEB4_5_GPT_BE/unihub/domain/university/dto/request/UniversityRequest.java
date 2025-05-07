package com.WEB4_5_GPT_BE.unihub.domain.university.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UniversityRequest(
        @NotBlank(message = "대학 이름은 필수입니다.") @Size(max = 100, message = "대학 이름은 100자를 초과할 수 없습니다.")
        String name,
        @NotBlank(message = "대학 이메일은 필수입니다.") @Email(message = "유효한 이메일 주소를 입력해주세요") @Size(max = 100, message = "대학 이름은 100자를 초과할 수 없습니다.")
        String emailDomain) {
}
