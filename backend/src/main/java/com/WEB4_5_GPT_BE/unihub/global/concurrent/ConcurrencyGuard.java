package com.WEB4_5_GPT_BE.unihub.global.concurrent;

import org.springframework.context.annotation.Profile;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * ConcurrencyGuard 애노테이션은 메서드 실행 전 분산 락을 사용해
 * 동시 접근을 제어합니다.
 */
@Documented
@Profile("!test")
@Target(METHOD)            // 해당 애노테이션을 메서드에만 적용할 수 있도록 지정
@Retention(RUNTIME)        // 런타임 시점까지 애노테이션 정보를 유지하여 AOP 등에서 활용할 수 있게 함
public @interface ConcurrencyGuard {
    String lockName(); // 락을 식별할 이름(key)을 지정

    long waitTime() default 5L; // 락 획득을 시도할 최대 대기 시간

    long leaseTime() default 3L; // 락을 획득한 이후 유지할 시간, 해당 시간이 지나면 락이 자동 해제됨

    TimeUnit timeUnit() default TimeUnit.SECONDS; // waitTime과 leaseTime에 사용될 시간 단위
}
