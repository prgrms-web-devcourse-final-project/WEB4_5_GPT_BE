package com.WEB4_5_GPT_BE.unihub.global.exception.enrollment;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

/**
 * 존재하지 않는 수강신청 기간에 대해 조회할 경우 발생하는 예외입니다.
 */
public class EnrollmentPeriodNotFoundException extends UnihubException {
    public EnrollmentPeriodNotFoundException() {
        super("404", "수강신청 기간 정보가 없습니다.");
    }
}
