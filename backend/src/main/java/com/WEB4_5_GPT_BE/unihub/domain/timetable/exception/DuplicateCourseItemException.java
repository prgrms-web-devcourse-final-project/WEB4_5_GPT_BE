package com.WEB4_5_GPT_BE.unihub.domain.timetable.exception;

/**
 * 이미 등록된 강의를 다시 추가하려고 할 때 발생하는 예외
 */
public class DuplicateCourseItemException extends TimetableException {
    public DuplicateCourseItemException() {
        super("409", "해당 강의는 이미 시간표에 등록되어 있습니다.");
    }
}
