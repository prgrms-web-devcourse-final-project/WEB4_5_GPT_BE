package com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.AuthException;

public class PasswordMismatchException extends AuthException {
    public PasswordMismatchException() {
        super("400", "현재 비밀번호가 일치하지 않습니다.");
    }
}