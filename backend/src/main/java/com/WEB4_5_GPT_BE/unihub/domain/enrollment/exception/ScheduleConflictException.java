package com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

/**
 * 기존 신청한 강의와 시간이 겹치는 경우 발생하는 예외입니다.
 */
public class ScheduleConflictException extends UnihubException {
    public ScheduleConflictException() {
        super("409", "기존 신청한 강의와 시간이 겹칩니다.");
    }
}
