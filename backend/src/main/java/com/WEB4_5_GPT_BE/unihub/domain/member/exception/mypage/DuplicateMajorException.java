package com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage;

import com.WEB4_5_GPT_BE.unihub.global.exception.AuthException;

public class DuplicateMajorException extends AuthException {
    public DuplicateMajorException() {
        super("400", "현재 전공과 동일합니다.");
    }
}