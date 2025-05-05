package com.WEB4_5_GPT_BE.unihub.global.exception.enrollment;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

/**
 * 학생이 해당 강좌를 신청한 기록 자체가 없는 경우 반환하는 예외입니다.
 */
public class EnrollmentNotFoundException extends UnihubException {
    public EnrollmentNotFoundException() {
        super("404", "수강신청 내역이 존재하지 않습니다.");
    }
}
