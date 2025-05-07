package com.WEB4_5_GPT_BE.unihub.domain.member.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

public class MemberException extends UnihubException {
    protected MemberException(String code, String message) {
        super(code, message);
    }
}