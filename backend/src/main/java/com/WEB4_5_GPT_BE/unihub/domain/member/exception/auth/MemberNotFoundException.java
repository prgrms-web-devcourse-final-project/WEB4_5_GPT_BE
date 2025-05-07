package com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.AuthException;

public class MemberNotFoundException extends AuthException {
    public MemberNotFoundException() {
        super("404", "존재하지 않는 회원입니다.");
    }
}
