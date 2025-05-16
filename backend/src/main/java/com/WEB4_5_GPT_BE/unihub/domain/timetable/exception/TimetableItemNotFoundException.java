package com.WEB4_5_GPT_BE.unihub.domain.timetable.exception;

/**
 * 시간표 항목을 찾을 수 없는 경우 발생하는 예외
 */
public class TimetableItemNotFoundException extends TimetableException {
    public TimetableItemNotFoundException() {
        super("404", "해당 시간표 항목을 찾을 수 없습니다.");
    }
}
