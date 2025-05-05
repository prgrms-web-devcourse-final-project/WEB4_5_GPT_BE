package com.WEB4_5_GPT_BE.unihub.global.exception.enrollment;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

/**
 * 수강신청 기간은 설정되어 있으나, 요청 날짜가 그 기간에 포함되지 않는 경우 반환하는 예외입니다.
 */
public class EnrollmentPeriodClosedException extends UnihubException {
    public EnrollmentPeriodClosedException() {
        super("403", "현재 수강신청 기간이 아닙니다.");
    }
}
