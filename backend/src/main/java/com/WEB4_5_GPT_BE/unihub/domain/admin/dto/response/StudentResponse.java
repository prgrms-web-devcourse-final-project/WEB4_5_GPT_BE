package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response;

import java.time.LocalDateTime;

/**
 * 학생 응답 DTO
 */
public record StudentResponse(
        Long memberId,
        String universityName,
        String memberName,
        String studentCode,
        String majorName,
        Integer grade,
        Integer semester,
        LocalDateTime createdAt
) {
}
