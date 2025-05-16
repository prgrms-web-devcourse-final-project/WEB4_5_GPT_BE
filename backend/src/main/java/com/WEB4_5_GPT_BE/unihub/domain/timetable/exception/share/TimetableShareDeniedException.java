package com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.share;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.TimetableException;

public class TimetableShareDeniedException extends TimetableException {

    public TimetableShareDeniedException() {
        super("403", "본인의 시간표만 공유할 수 있습니다.");
    }
}