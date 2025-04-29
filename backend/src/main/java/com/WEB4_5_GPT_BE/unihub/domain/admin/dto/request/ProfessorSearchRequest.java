package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;

/**
 * 교직원 목록 검색 조건
 */
public record ProfessorSearchRequest(
        Long universityId,
        String professorName,
        Long majorId,
        ApprovalStatus status
) {
}
