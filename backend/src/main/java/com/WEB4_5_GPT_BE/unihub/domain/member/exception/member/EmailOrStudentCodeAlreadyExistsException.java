package com.WEB4_5_GPT_BE.unihub.domain.member.exception.member;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.MemberException;

public class EmailOrStudentCodeAlreadyExistsException extends MemberException {
    public EmailOrStudentCodeAlreadyExistsException() {
        super("409", "이메일 또는 학번이 이미 등록되어 있습니다.");
    }
}