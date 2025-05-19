package com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception;

import org.springframework.http.HttpStatus;

public class NoSessionException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "수강신청 세션이 필요합니다. 대기열에 참여해주세요.";
    private final HttpStatus status;

    public NoSessionException() {
        super(DEFAULT_MESSAGE);
        this.status = HttpStatus.FORBIDDEN;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
