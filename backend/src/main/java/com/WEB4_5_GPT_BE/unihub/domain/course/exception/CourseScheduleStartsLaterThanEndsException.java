package com.WEB4_5_GPT_BE.unihub.domain.course.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

public class CourseScheduleStartsLaterThanEndsException extends UnihubException {
    public CourseScheduleStartsLaterThanEndsException() {
        super("400", "수업 시작 시각이 종료 시각보다 늦습니다.");
    }
}
