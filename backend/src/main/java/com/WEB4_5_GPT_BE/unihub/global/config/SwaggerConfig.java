package com.WEB4_5_GPT_BE.unihub.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UniHub API 명세서")
                        .version("v1.0.0")
                        .description("""
                                ### 📌 공통 오류 응답 안내

                                UniHub의 모든 API는 다음과 같은 공통 오류 응답 포맷을 따릅니다.  
                                (HTTP 상태코드와 응답 바디 내 `"code"` 필드는 다를 수 있습니다.)

                                #### 🔁 공통 응답 JSON 형식
                                ```json
                                {
                                  "code": "에러코드 문자열 (예: 401, 401-1)",
                                  "message": "오류 메시지",
                                  "data": null
                                }
                                ```

                                #### ❗ 오류 코드 목록

                                | HTTP Status | code (문자열) | 의미 |
                                |-------------|----------------|------|
                                | 400         | "400"           | 잘못된 요청 (본문 오류 등) |
                                | 401         | "401"           | 인증 실패 – 로그인 필요 |
                                | 401         | "401-1"         | AccessToken 만료됨 |
                                | 403         | "403"           | 권한 없음 |
                                | 404         | "404"           | 존재하지 않는 사용자 |
                                | 500         | "500"           | 서버 오류 |

                                #### 📦 예시
                                ```json
                                {
                                  "code": "401-1",
                                  "message": "AccessToken이 만료되었습니다.",
                                  "data": null
                                }
                                ```
                                """));
    }
}
