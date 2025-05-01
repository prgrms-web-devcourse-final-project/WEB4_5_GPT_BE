package com.WEB4_5_GPT_BE.unihub.domain.member.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.AuthException;

public class InvalidRefreshTokenException extends AuthException {
    public InvalidRefreshTokenException() {
        super("401", "유효하지 않은 refreshToken입니다.");
    }
}
