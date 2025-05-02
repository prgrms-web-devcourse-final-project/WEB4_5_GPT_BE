package com.WEB4_5_GPT_BE.unihub.global.exception;

public abstract class AuthException extends UnihubException {
    protected AuthException(String code, String message) {
        super(code, message);
    }
}