package com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.share;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.TimetableException;

public class TimetablePrivateException extends TimetableException {
    public TimetablePrivateException() {
        super("403", "해당 시간표는 비공개입니다.");
    }
}