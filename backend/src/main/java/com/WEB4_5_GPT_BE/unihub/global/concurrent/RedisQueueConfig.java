package com.WEB4_5_GPT_BE.unihub.global.concurrent;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.cancel.EnrollmentCancelCommand;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.enroll.EnrollmentCommand;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("!test")
@Configuration
public class RedisQueueConfig {
    /**
     * 수강신청 요청을 위한 Redis BlockingQueue
     */
    @Bean
    public RBlockingQueue<EnrollmentCommand> enrollQueue(RedissonClient redisson) {
        return redisson.getBlockingQueue("enrollQueue");
    }

    /**
     * 수강취소 요청을 위한 Redis BlockingQueue
     */
    @Bean
    public RBlockingQueue<EnrollmentCancelCommand> cancelQueue(RedissonClient redisson) {
        return redisson.getBlockingQueue("cancelQueue");
    }
}
