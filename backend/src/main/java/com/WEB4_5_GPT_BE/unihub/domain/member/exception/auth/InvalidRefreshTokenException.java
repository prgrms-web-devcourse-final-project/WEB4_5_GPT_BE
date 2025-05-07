package com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.AuthException;

public class InvalidRefreshTokenException extends AuthException {
    public InvalidRefreshTokenException() {
        super("401", "유효하지 않은 refreshToken입니다.");
    }
}
