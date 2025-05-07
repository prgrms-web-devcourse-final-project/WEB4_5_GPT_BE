package com.WEB4_5_GPT_BE.unihub.domain.member.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

public abstract class AuthException extends UnihubException {
    protected AuthException(String code, String message) {
        super(code, message);
    }
}