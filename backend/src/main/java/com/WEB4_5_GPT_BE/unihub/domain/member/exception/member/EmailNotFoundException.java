package com.WEB4_5_GPT_BE.unihub.domain.member.exception.member;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.MemberException;

public class EmailNotFoundException extends MemberException {
    public EmailNotFoundException() {
        super("404", "등록되지 않은 이메일 주소입니다.");
    }
}