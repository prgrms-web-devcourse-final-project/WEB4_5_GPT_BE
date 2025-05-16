package com.WEB4_5_GPT_BE.unihub.domain.university.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

public class UniversityNotFoundException extends UnihubException {
    public UniversityNotFoundException() {
        super("404", "존재하지 않는 대학교입니다.");
    }
}
