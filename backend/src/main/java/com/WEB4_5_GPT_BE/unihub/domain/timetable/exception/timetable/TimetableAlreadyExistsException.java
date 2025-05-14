package com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.timetable;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.TimetableException;

public class TimetableAlreadyExistsException extends TimetableException {
    public TimetableAlreadyExistsException() {
        super("409", "해당 학기 시간표는 이미 존재합니다.");
    }
}