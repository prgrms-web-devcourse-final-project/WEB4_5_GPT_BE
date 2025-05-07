package com.WEB4_5_GPT_BE.unihub.domain.member.exception.member;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.MemberException;

public class InvalidEmailDomainException extends MemberException {
    public InvalidEmailDomainException(String expectedDomain) {
        super("400", "선택한 학교의 이메일 형식(@" + expectedDomain + ")과 일치하지 않습니다.");
    }
}