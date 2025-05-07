package com.WEB4_5_GPT_BE.unihub.domain.member.exception.member;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.MemberException;

public class EmailAlreadyVerifiedException extends MemberException {
    public EmailAlreadyVerifiedException() {
        super("400", "이메일은 이미 인증되었습니다.");
    }
}