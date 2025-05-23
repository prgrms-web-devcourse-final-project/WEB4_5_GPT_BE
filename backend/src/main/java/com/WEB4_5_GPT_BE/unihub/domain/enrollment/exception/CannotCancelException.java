package com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

public class CannotCancelException extends UnihubException {
    public CannotCancelException() {
        super("400", "정원이 0 이하로 내려갈 수 없습니다.");
    }
}