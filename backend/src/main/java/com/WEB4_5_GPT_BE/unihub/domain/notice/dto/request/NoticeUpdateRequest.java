package com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record NoticeUpdateRequest(
        @NotBlank(message = "제목은 필수입니다.") String title,
        @NotBlank(message = "본문은 필수입니다.") String content,
        String attachmentUrl
) {}
