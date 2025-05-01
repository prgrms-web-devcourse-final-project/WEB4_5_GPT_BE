package com.WEB4_5_GPT_BE.unihub.domain.member.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.AuthException;

public class InvalidAccessTokenException extends AuthException {
    public InvalidAccessTokenException() {
        super("401", "유효하지 않은 형식.");
    }
}
