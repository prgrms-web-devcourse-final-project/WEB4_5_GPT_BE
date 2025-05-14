package com.WEB4_5_GPT_BE.unihub.domain.timetable.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

public abstract class TimetableException extends UnihubException {
    protected TimetableException(String code, String message) {
        super(code, message);
    }
}