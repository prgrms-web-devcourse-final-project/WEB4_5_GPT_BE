package com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

/**
 * 학점 한도를 초과하여 수강신청할 수 없는 경우 반환하는 예외입니다.
 */
public class CreditLimitExceededException extends UnihubException {
    public CreditLimitExceededException() {
        super("409", "학점 한도를 초과하여 수강신청할 수 없습니다.");
    }
}
