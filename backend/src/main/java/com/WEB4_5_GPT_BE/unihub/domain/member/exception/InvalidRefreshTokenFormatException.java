package com.WEB4_5_GPT_BE.unihub.domain.member.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.AuthException;

public class InvalidRefreshTokenFormatException extends AuthException {
    public InvalidRefreshTokenFormatException() {
        super("400", "잘못된 refreshToken 형식입니다.");
    }
}
