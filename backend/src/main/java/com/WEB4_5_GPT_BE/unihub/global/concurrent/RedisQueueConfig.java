package com.WEB4_5_GPT_BE.unihub.global.concurrent;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.EnrollmentCommand;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.cancel.EnrollmentCancelCommand;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisQueueConfig {

//    @Bean
//    public RAtomicLong capacityCounter(RedissonClient redisson) {
//        // 애플리케이션 기동/강의 생성 시점에
//        // 각 courseId:capacity 키를 초기화해야 합니다.
//        // 예: redisson.getAtomicLong("course:11:capacity").set(30);
//        return null;
//    }

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
