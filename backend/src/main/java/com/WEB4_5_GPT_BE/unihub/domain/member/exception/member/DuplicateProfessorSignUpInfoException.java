package com.WEB4_5_GPT_BE.unihub.domain.member.exception.member;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.MemberException;

public class DuplicateProfessorSignUpInfoException extends MemberException {
    public DuplicateProfessorSignUpInfoException() {
        super("409", "이메일 또는 사번이 이미 등록되어 있습니다.");
    }
}