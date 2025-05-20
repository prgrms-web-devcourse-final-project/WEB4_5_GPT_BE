package com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

/**
 * 동일 강좌를 중복 신청(따닥)하는 경우 반환하는 예외입니다.
 */
public class RequestAlreadyQueuedException extends UnihubException {
    public RequestAlreadyQueuedException() {
        super("409", "이미 요청을 처리중입니다.");
    }
}
