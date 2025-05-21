package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.enroll;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception.RequestAlreadyQueuedException;
import com.WEB4_5_GPT_BE.unihub.global.concurrent.ConcurrencyGuard;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 수강신청 중복 검사 컴포넌트
 *
 * 학생(studentId)과 강좌(courseId) 조합에 대해
 * 이미 enqueue 되어있는지 flag를 통해 검사하고,
 * 중복 등록 시 예외를 던집니다.
 *
 * @ConcurrencyGuard(lockName = "student:enroll") 어노테이션을 통해
 * 분산 락을 적용하여 동시성 문제를 방지합니다.
 */
@Component
@RequiredArgsConstructor
public class EnrollmentDuplicateChecker {

    private final RedissonClient redisson;

    /**
     * 지정된 학생과 강좌 조합에 대해
     * Redis에 플래그가 없으면 설정하고(1시간 유효),
     * 이미 존재하면 DuplicateEnrollmentException을 발생시킵니다.
     *
     * @param studentId 학생 ID
     * @param courseId  강좌 ID
     * @throws RequestAlreadyQueuedException 이미 대기 중인 경우 발생
     */
    @ConcurrencyGuard(lockName = "student:enroll")
    public void markEnqueuedIfAbsent(Long studentId, Long courseId) {
        // Redis 키: enroll:queued:{studentId}:{courseId}
        String flagKey = "enroll:queued:" + studentId + ":" + courseId;
        RBucket<Boolean> flag = redisson.getBucket(flagKey);

        // 이미 플래그가 설정되어 있으면 중복으로 간주
        if (flag.isExists()) {
            throw new RequestAlreadyQueuedException();
        }

        // 플래그가 없으면 true로 설정하고 1시간 뒤 자동 만료
        flag.set(true, Duration.ofHours(1));
    }
}
