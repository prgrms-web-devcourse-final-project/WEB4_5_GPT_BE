package com.WEB4_5_GPT_BE.unihub.domain.notice.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.notice.entity.Notice;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NoticeListResponse(
        Long id,
        String title,
        LocalDateTime createdAt
) {
    public static NoticeListResponse from(Notice notice) {
        return NoticeListResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
