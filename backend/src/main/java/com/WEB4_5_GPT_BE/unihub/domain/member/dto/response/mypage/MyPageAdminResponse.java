package com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MyPageAdminResponse(
        Long id,
        String email,
        String name,
        Role role,
        LocalDateTime createdAt
) {}