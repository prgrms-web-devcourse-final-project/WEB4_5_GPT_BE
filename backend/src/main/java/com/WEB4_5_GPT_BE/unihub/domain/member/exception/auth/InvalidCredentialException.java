package com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.AuthException;

public class InvalidCredentialException extends AuthException {
    public InvalidCredentialException() {
        super("401", "이메일 또는 비밀번호가 잘못되었습니다.");
    }
}