package com.WEB4_5_GPT_BE.unihub.global.exception.home;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

public class HomeNoDataException extends UnihubException {
    public HomeNoDataException(String code, String message) {
        super(code, message);
    }
}
