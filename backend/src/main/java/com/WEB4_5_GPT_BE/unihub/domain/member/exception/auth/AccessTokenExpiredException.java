package com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth;

import org.springframework.security.core.AuthenticationException;

public class AccessTokenExpiredException extends AuthenticationException {
    // Spring Security의 ExceptionTranslationFilter에서 처리되도록 하기 위해
    // RuntimeException이 아닌 AuthenticationException을 상속받았습니다.
    // (UnihubException을 상속하면 SecurityFilterChain에서 401 처리를 할 수 없습니다.)

    public AccessTokenExpiredException() {
        super("AccessToken이 만료되었습니다."); // 메시지만 넘김
    }
}