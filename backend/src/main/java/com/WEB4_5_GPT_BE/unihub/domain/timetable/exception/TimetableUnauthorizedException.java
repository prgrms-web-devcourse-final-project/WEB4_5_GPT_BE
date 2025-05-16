package com.WEB4_5_GPT_BE.unihub.domain.timetable.exception;

/**
 * 시간표 소유자가 아닌 사용자가 접근 시 발생하는 예외
 */
public class TimetableUnauthorizedException extends TimetableException {
    public TimetableUnauthorizedException() {
        super("401", "잘못된 사용자입니다.");
    }
}
