package com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.AuthException;

public class LoginLockedException extends AuthException {
    public LoginLockedException() {
        super("429", "비밀번호 오류 5회 이상. 5분간 로그인이 제한됩니다.");
    }
}
