package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.QueueStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentQueueService {

    private static final String WAITING_QUEUE_KEY = "enrollment:waiting-queue";
    private static final String ACTIVE_USERS_KEY = "enrollment:active-users";
    private static final String SESSION_PREFIX = "enrollment:session:";
    private static final int MAX_CONCURRENT_USERS = 1000;
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(30);
    private final StringRedisTemplate redisTemplate;
    private final SseEmitterService sseEmitterService;

    /**
     * 대기열에 사용자 추가
     */
    public QueueStatusDto addToQueue(String memberId) {
        // 이미 활성 세션이 있는지 확인
        Boolean hasSession = redisTemplate.hasKey(SESSION_PREFIX + memberId);
        if (Boolean.TRUE.equals(hasSession)) {
            QueueStatusDto status = new QueueStatusDto(true, 0, 0);
            sseEmitterService.sendQueueStatus(memberId, status);
            return status;
        }

        // 이미 대기열에 있는지 확인
        if (isUserInQueue(memberId)) {
            int position = getPositionInQueue(memberId);
            int waitTime = position * 5; // 사용자당 5초 소요 가정
            QueueStatusDto status = new QueueStatusDto(false, position, waitTime);
            sseEmitterService.sendQueueStatus(memberId, status);
            return status;
        }

        // 현재 활성 사용자 수 확인
        Long activeUsers = redisTemplate.opsForValue().increment(ACTIVE_USERS_KEY, 0);

        // 여유 자리가 있으면 바로 접속 허용
        if (activeUsers < MAX_CONCURRENT_USERS) {
            redisTemplate.opsForValue().increment(ACTIVE_USERS_KEY);
            redisTemplate.opsForValue().set(SESSION_PREFIX + memberId, "active", SESSION_TIMEOUT);
            log.info("사용자 {} 즉시 접속 허용", memberId);

            QueueStatusDto status = new QueueStatusDto(true, 0, 0);
            sseEmitterService.sendQueueStatus(memberId, status);
            return status;
        }

        // 대기열에 추가
        redisTemplate.opsForList().rightPush(WAITING_QUEUE_KEY, memberId);
        int position = getPositionInQueue(memberId);
        int waitTime = position * 30; // 사용자당 30초 소요 가정

        log.info("사용자 {} 대기열 추가, 위치: {}, 예상 대기시간: {}초", memberId, position, waitTime);
        QueueStatusDto status = new QueueStatusDto(false, position, waitTime);
        sseEmitterService.sendQueueStatus(memberId, status);
        return status;
    }

    /**
     * 현재 대기열 상태 조회
     */
    public QueueStatusDto getQueueStatus(String memberId) {
        // 활성 세션 확인
        Boolean hasSession = redisTemplate.hasKey(SESSION_PREFIX + memberId);
        if (Boolean.TRUE.equals(hasSession)) {
            return new QueueStatusDto(true, 0, 0);
        }

        // 대기열 위치 확인
        int position = getPositionInQueue(memberId);
        if (position == 0) {
            // 대기열에 없으면 추가
            return addToQueue(memberId);
        }

        int waitTime = position * 5; // 사용자당 5초 소요 가정
        return new QueueStatusDto(false, position, waitTime);
    }

    /**
     * 사용자 세션 종료
     */
    public void releaseSession(String memberId) {
        Boolean hasSession = redisTemplate.hasKey(SESSION_PREFIX + memberId);
        if (Boolean.TRUE.equals(hasSession)) {
            redisTemplate.delete(SESSION_PREFIX + memberId);
            redisTemplate.opsForValue().decrement(ACTIVE_USERS_KEY);
            log.info("사용자 {} 세션 종료", memberId);
        }
    }

    /**
     * 대기열에 사용자가 있는지 확인
     */
    public boolean isUserInQueue(String userId) {
        return getPositionInQueue(userId) > 0;
    }

    /**
     * 대기열에서 사용자 위치 확인
     */
    public int getPositionInQueue(String userId) {
        List<String> queueMembers = redisTemplate.opsForList().range(WAITING_QUEUE_KEY, 0, -1);

        for (int i = 0; i < queueMembers.size(); i++) {
            if (userId.equals(queueMembers.get(i))) {
                return i + 1; // 1부터 시작하는 위치 반환
            }
        }

        return 0; // 대기열에 없음
    }
}
