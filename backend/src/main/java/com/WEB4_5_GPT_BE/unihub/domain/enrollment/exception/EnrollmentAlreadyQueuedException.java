package com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

/**
 * 동일 강좌를 중복 신청(따닥)하는 경우 반환하는 예외입니다.
 */
public class EnrollmentAlreadyQueuedException extends UnihubException {
    public EnrollmentAlreadyQueuedException() {
        super("409", "이미 수강신청 처리중입니다.");
    }
}
