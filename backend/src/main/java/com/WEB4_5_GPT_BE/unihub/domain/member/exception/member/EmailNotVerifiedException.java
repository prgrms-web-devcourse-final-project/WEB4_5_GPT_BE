package com.WEB4_5_GPT_BE.unihub.domain.member.exception.member;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.MemberException;

public class EmailNotVerifiedException extends MemberException {
    public EmailNotVerifiedException() {
        super("400", "이메일 인증을 완료해주세요.");
    }
}