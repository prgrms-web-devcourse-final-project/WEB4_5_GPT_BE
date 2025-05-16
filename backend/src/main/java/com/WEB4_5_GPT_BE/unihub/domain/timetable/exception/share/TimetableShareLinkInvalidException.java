package com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.share;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.TimetableException;

public class TimetableShareLinkInvalidException extends TimetableException {
    public TimetableShareLinkInvalidException() {
        super("404", "공유 링크가 만료되었거나 존재하지 않습니다.");
    }
}

