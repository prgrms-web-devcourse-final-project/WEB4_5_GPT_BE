package com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

/**
 * 필수 쿼리 파라미터가 누락되었을 때 발생하는 예외
 */
public class RequiredParameterMissingException extends UnihubException {
    public RequiredParameterMissingException(String paramName) {
        super("400", String.format("필수 쿼리 파라미터 '%s'가 누락되었습니다.", paramName));
    }
}
