package com.WEB4_5_GPT_BE.unihub.global.concurrent;

import com.WEB4_5_GPT_BE.unihub.global.concurrent.exception.ConcurrencyLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@Profile("!test")
@RequiredArgsConstructor
public class ConcurrencyGuardAspect {

    private final RedissonClient redissonClient;
    private final TransactionAspect transactionAspect;

    /**
     * 분산 락을 걸고, 새로운 트랜잭션에서 비즈니스 로직을 수행한 뒤 락을 해제합니다.
     */
    @Around("@annotation(ConcurrencyGuard) && (args(..))")
    public Object handleConcurrency(ProceedingJoinPoint joinPoint) throws Throwable {

        ConcurrencyGuard annotation = getAnnotation(joinPoint); // 1) 어노테이션 설정 정보 가져오기

        // 2) 메서드 인자를 가져온 뒤 해당 정보로 락 이름을 생성
        String lockName = getLockNameById(joinPoint.getArgs(), annotation);

        // 해당 lockName으로 된 Redisson 분산 락 객체 생성
        RLock lock = redissonClient.getLock(lockName);

        try {
            // 3) 락 획득 시도 (대기시간 waitTime, 유지시간 leaseTime)
            boolean available = lock.tryLock(annotation.waitTime(), annotation.leaseTime(), annotation.timeUnit());
            if (!available) {
                throw new ConcurrencyLockException(); // 락을 얻지 못하면 예외 발생
            }
            // 락을 획득하게 되면 해당 비즈니스 로직이 새로운 트랜잭션에서 실행될 수 있도록
            // transactionAspect에 넘겨줌
            return transactionAspect.proceed(joinPoint);
        } finally {
            try {
                lock.unlock(); // 5) 락 해제
            } catch (IllegalMonitorStateException e) {
                log.warn("Redisson 락이 이미 해제되었습니다 lockName: " + lockName); // 이미 해제된 경우 경고 로그
            }
        }
    }

    /**
     * 실행 되는 메서드에 존재하는 어노테이션 정보를 가져와
     * ConcurrencyGuard 어노테이션 정보를 추출합니다.
     */
    private ConcurrencyGuard getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(ConcurrencyGuard.class);
    }

    /**
     * 락 이름을 "lock:{lockName}:{studentId}:{courseId}" 형식으로 생성합니다.
     */
    private String getLockNameById(Object[] args, ConcurrencyGuard annotation) {
        String lockNameFormat = "lock:%s:%s:%s";
        String lockName = annotation.lockName();
        String studentId = args[0].toString();
        String courseId = args[1].toString();
        String formatted = lockNameFormat.formatted(lockName, studentId, courseId);
        System.out.println(formatted);
        return formatted;
    }
}
