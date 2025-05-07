package com.WEB4_5_GPT_BE.unihub.global.config.springDoc;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI에서 사용할 보안 스키마를 정의합니다.
 * accessToken을 사용한 Bearer 인증 방식을 설정합니다.
 */
@Configuration
@SecurityScheme(
        name = "accessToken을 사용한 bearerAuth 로그인 인증",
        type = SecuritySchemeType.HTTP,             // HTTP 방식의 인증
        scheme = "bearer",                          // Bearer 인증 방식
        bearerFormat = "JWT",                       // JWT 형식의 Bearer 토큰
        in = SecuritySchemeIn.HEADER                // HTTP 헤더에서 인증 정보 포함
)
public class SpringDocSecurityConfig {
}
