package com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.AuthException;

public class RefreshTokenNotFoundException extends AuthException {
    public RefreshTokenNotFoundException() {
        super("401", "Refresh Token이 존재하지 않습니다.");
    }
}