package com.WEB4_5_GPT_BE.unihub.domain.member.dto.request;

import jakarta.validation.constraints.NotEmpty;

public record TokenRefreshRequest(@NotEmpty String refreshToken) {}
