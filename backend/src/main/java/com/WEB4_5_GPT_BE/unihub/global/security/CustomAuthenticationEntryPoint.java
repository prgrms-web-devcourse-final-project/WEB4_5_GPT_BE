package com.WEB4_5_GPT_BE.unihub.global.security;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth.AccessTokenExpiredException;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import com.WEB4_5_GPT_BE.unihub.global.util.Ut;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String code = "401";
        String message = "로그인이 필요합니다.";

        if (authException instanceof AccessTokenExpiredException) {
            code = "401-1";
            message = authException.getMessage(); // "AccessToken이 만료되었습니다."
        }

        response.getWriter().write(Ut.Json.toString(new RsData<>(code, message)));
    }
}