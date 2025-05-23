package com.WEB4_5_GPT_BE.unihub.global.config.redisson;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 동시성 제어를 위해 Redisson을 사용하기 위한 설정 클래스입니다.
 * Redis 서버에 연결하기 위한 RedissonClient를 생성합니다.
 */
@Configuration
public class RedissonConfig {
    @Value("${spring.data.redis.host}") // Redis 호스트 주소
    private String redisHost;

    @Value("${spring.data.redis.port}") // Redis 포트 번호
    private int redisPort;

    @Value("${spring.data.redis.password:}") // Redis 비밀번호 (없을 경우 빈 문자열)
    private String redisPassword;

    private static final String REDISSON_HOST_PREFIX = "redis://";

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                // Redis 서버 주소 설정
                .setAddress(REDISSON_HOST_PREFIX + redisHost + ":" + redisPort)
                // 비밀번호가 있을 때만 설정 (로컬/배포 환경 모두 대응)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword);
        return Redisson.create(config);
    }
}
