package com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.timetable;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.TimetableException;

public class TimetableNotFoundException extends TimetableException {
    public TimetableNotFoundException() {
        super("404", "해당 연도와 학기의 시간표가 존재하지 않습니다.");
    }
}