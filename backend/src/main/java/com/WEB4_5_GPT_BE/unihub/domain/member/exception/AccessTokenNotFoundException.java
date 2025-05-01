package com.WEB4_5_GPT_BE.unihub.domain.member.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.AuthException;

public class AccessTokenNotFoundException extends AuthException {
    public AccessTokenNotFoundException() {
        super("401", "인증이 필요합니다.");
    }
}
