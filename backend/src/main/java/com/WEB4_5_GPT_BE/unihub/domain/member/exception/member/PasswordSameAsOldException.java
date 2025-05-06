package com.WEB4_5_GPT_BE.unihub.domain.member.exception.member;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.MemberException;

public class PasswordSameAsOldException extends MemberException {
    public PasswordSameAsOldException() {
        super("400", "기존 비밀번호와 동일한 비밀번호로는 변경할 수 없습니다.");
    }
}