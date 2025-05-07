package com.WEB4_5_GPT_BE.unihub.domain.member.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;

public record MemberLoginResponse(String accessToken, String refreshToken, Long id, String email, Role role) {}
