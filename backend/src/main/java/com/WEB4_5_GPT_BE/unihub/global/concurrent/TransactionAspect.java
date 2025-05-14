package com.WEB4_5_GPT_BE.unihub.global.concurrent;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * [분산 락 적용 시 내 비즈니스 로직을 독립 트랜잭션으로 실행하기 위한 Aspect]
 * <p>
 * 분산 락 처리와는 별도로 새로운 트랜잭션을 시작하여 비즈니스 로직을 처리하도록 합니다.
 * 락 → (새 트랜잭션) → 비즈니스 로직 실행 → (새 트랜잭션 커밋) → 락 해제
 * 순서를 확실히 분리함으로써 다른 스레드는 커밋이 완료된 데이터만을 사용할 수 있게되며
 * 이를 통해 데이터 정합성을 보장할 수 있습니다.
 */
@Component
public class TransactionAspect {
    /**
     * [propagation = REQUIRES_NEW]
     * - 부모 트랜잭션이 존재해도 이를 일시 중단시키고 새로운 트랜잭션을 생성
     * <p>
     * [timeout = 2]
     * - 지정된 시간 내에 커밋되지 않으면 롤백 발생
     * - 락 해제 시점(leaseTime=3L)보다 트랜잭션 강제종료(timeout=2L) 시점이 짧도록 설정하여
     * 락이 자동 해제되기 전에 트랜잭션이 끝나도록 설정합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 2)
    public Object proceed(final ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed(); // 실제 비즈니스 로직(원본 메서드) 실행
    }
}
