package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request;

/**
 * 학생 목록 검색 조건
 */
public record StudentSearchRequest(
        Long universityId, Long majorId, Integer grade, Integer semester) {
}
