package com.WEB4_5_GPT_BE.unihub.domain.member.exception.member;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.MemberException;

public class EmailSendFailureException extends MemberException {
    public EmailSendFailureException() {
        super("500", "이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
    }
}