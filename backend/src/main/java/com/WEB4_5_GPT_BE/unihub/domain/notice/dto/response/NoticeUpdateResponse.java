package com.WEB4_5_GPT_BE.unihub.domain.notice.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.notice.entity.Notice;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NoticeUpdateResponse(
        Long id,
        String title,
        String content,
        String attachmentUrl,
        LocalDateTime modifiedAt
) {
    public static NoticeUpdateResponse from(Notice notice) {
        return NoticeUpdateResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .attachmentUrl(notice.getAttachmentUrl())
                .modifiedAt(notice.getModifiedAt())
                .build();
    }
}
