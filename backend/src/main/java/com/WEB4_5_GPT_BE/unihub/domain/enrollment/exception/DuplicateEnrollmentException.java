package com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

/**
 * 동일한 강의에 중복 신청을 시도할 때 발생하는 예외입니다.
 */
public class DuplicateEnrollmentException extends UnihubException {
    public DuplicateEnrollmentException() {
        super("409", "이미 신청한 강의입니다.");
    }
}
