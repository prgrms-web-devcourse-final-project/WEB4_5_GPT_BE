package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;

import java.time.LocalDateTime;

/**
 * 교직원 응답 DTO
 */
public record ProfessorResponse(
        Long memberId,
        String universityName,
        String memberName,
        String majorName,
        ApprovalStatus status,
        LocalDateTime createdAt
) {
}
