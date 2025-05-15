package com.WEB4_5_GPT_BE.unihub.domain.university.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

public class MajorNotFoundException extends UnihubException {
    public MajorNotFoundException() {
        super("404", "존재하지 않는 대학교입니다.");
    }
}
