package com.WEB4_5_GPT_BE.unihub.domain.timetable.exception;

/**
 * 시간표를 찾을 수 없는 경우 발생하는 예외
 */
public class TimetableNotFoundException extends TimetableException {
    public TimetableNotFoundException() {
        super("404", "시간표를 찾을 수 없습니다.");
    }
}
