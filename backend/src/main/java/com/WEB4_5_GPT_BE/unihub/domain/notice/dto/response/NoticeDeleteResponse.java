package com.WEB4_5_GPT_BE.unihub.domain.notice.dto.response;

public record NoticeDeleteResponse(
        String message
) {
    public static NoticeDeleteResponse from(String message) {
        return new NoticeDeleteResponse(message);
    }
}
