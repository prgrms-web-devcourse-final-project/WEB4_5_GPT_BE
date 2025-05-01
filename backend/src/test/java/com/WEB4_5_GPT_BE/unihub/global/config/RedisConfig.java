package com.WEB4_5_GPT_BE.unihub.global.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;

@TestConfiguration
@ActiveProfiles("test")
public class RedisConfig {

    /**
     * Redis TestContainer 설정
     */
    private static final GenericContainer<?> redisTestContainer =
            new GenericContainer<>("redis:latest")
                    .withExposedPorts(6379)
                    .withReuse(true);

    static {
        redisTestContainer.start();
        System.setProperty("spring.data.redis.host", redisTestContainer.getHost());
        System.setProperty("spring.data.redis.port", redisTestContainer.getMappedPort(6379).toString());
    }

    @Bean
    public GenericContainer<?> redisContainer() {
        return redisTestContainer;
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                redisTestContainer.getHost(), redisTestContainer.getMappedPort(6379)
        );
        factory.setValidateConnection(true);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    // 추후 새로운 redisTemplate 적용 시 주석 해제하여 사용
//    @Bean
//    @Primary
//    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
//        RedisTemplate<String, String> template = new RedisTemplate<>();
//        template.setConnectionFactory(redisConnectionFactory);
//
//        StringRedisSerializer stringSerializer = new StringRedisSerializer();
//        template.setKeySerializer(stringSerializer);
//        template.setValueSerializer(stringSerializer);
//        template.setHashKeySerializer(stringSerializer);
//        template.setHashValueSerializer(stringSerializer);
//
//        template.afterPropertiesSet();
//        return template;
//    }
//
//    @Bean
//    public RedisTemplate<String, Object> objectRedisTemplate(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//        redisTemplate.setConnectionFactory(connectionFactory);
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(String.class));
//        return redisTemplate;
//    }

}
