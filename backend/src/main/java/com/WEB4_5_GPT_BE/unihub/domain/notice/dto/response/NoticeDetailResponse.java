package com.WEB4_5_GPT_BE.unihub.domain.notice.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NoticeDetailResponse(
        Long id,
        String title,
        String content,
        String attachmentUrl,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
