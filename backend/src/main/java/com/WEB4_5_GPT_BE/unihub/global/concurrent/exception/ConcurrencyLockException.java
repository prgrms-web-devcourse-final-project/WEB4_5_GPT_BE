package com.WEB4_5_GPT_BE.unihub.global.concurrent.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

public class ConcurrencyLockException extends UnihubException {
    public ConcurrencyLockException() {
        super("500", "현재 요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    }
}