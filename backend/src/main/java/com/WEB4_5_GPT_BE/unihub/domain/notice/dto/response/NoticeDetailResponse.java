package com.WEB4_5_GPT_BE.unihub.domain.notice.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.notice.entity.Notice;
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
) {
    public static NoticeDetailResponse from(Notice notice) {
        return NoticeDetailResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .attachmentUrl(notice.getAttachmentUrl())
                .createdAt(notice.getCreatedAt())
                .modifiedAt(notice.getModifiedAt())
                .build();
    }
}
