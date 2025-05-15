package com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.share;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.TimetableException;

public class TimetablePrivateException extends TimetableException {
    public TimetablePrivateException() {
        super("403", "비공개 시간표는 본인만 열람할 수 있습니다.");
    }
}