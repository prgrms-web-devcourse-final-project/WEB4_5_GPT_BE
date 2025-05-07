package com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.AuthException;

public class RefreshTokenMismatchException extends AuthException {
    public RefreshTokenMismatchException() {
        super("401", "Refresh Token 정보가 일치하지 않습니다.");
    }
}
