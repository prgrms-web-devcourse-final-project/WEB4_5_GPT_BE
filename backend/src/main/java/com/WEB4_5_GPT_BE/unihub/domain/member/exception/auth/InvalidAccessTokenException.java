package com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.AuthException;

public class InvalidAccessTokenException extends AuthException {
    public InvalidAccessTokenException() {
        super("401", "유효하지 않은 형식.");
    }
}
