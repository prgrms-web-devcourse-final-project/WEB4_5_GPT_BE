package com.WEB4_5_GPT_BE.unihub.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // 비동기 처리를 위한 설정
    // 필요한 경우 여기에 ThreadPoolTaskExecutor 등을 설정할 수 있습니다.
}
