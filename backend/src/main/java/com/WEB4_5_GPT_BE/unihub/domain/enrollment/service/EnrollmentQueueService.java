package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.QueueStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
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
    public QueueStatusDto addToQueue(String userId) {
        // 이미 활성 세션이 있는지 확인
        Boolean hasSession = redisTemplate.hasKey(SESSION_PREFIX + userId);
        if (Boolean.TRUE.equals(hasSession)) {
            QueueStatusDto status = new QueueStatusDto(true, 0, 0);
            sseEmitterService.sendQueueStatus(userId, status);
            return status;
        }

        // 이미 대기열에 있는지 확인
        if (isUserInQueue(userId)) {
            int position = getPositionInQueue(userId);
            int waitTime = position * 5; // 사용자당 5초 소요 가정
            QueueStatusDto status = new QueueStatusDto(false, position, waitTime);
            sseEmitterService.sendQueueStatus(userId, status);
            return status;
        }

        // 현재 활성 사용자 수 확인
        Long activeUsers = redisTemplate.opsForValue().increment(ACTIVE_USERS_KEY, 0);

        // 여유 자리가 있으면 바로 접속 허용
        if (activeUsers < MAX_CONCURRENT_USERS) {
            redisTemplate.opsForValue().increment(ACTIVE_USERS_KEY);
            redisTemplate.opsForValue().set(SESSION_PREFIX + userId, "active", SESSION_TIMEOUT);
            log.info("사용자 {} 즉시 접속 허용", userId);

            QueueStatusDto status = new QueueStatusDto(true, 0, 0);
            sseEmitterService.sendQueueStatus(userId, status);
            return status;
        }

        // 대기열에 추가
        redisTemplate.opsForList().rightPush(WAITING_QUEUE_KEY, userId);
        int position = getPositionInQueue(userId);
        int waitTime = position * 5; // 사용자당 5초 소요 가정

        log.info("사용자 {} 대기열 추가, 위치: {}, 예상 대기시간: {}초", userId, position, waitTime);
        QueueStatusDto status = new QueueStatusDto(false, position, waitTime);
        sseEmitterService.sendQueueStatus(userId, status);
        return status;
    }

    /**
     * 현재 대기열 상태 조회
     */
    public QueueStatusDto getQueueStatus(String userId) {
        // 활성 세션 확인
        Boolean hasSession = redisTemplate.hasKey(SESSION_PREFIX + userId);
        if (Boolean.TRUE.equals(hasSession)) {
            return new QueueStatusDto(true, 0, 0);
        }

        // 대기열 위치 확인
        int position = getPositionInQueue(userId);
        if (position == 0) {
            // 대기열에 없으면 추가
            return addToQueue(userId);
        }

        int waitTime = position * 5; // 사용자당 5초 소요 가정
        return new QueueStatusDto(false, position, waitTime);
    }

    /**
     * 대기열에서 배치 처리하여 사용자를 활성 상태로 전환
     */
    public List<String> processBatch(int batchSize) {
        List<String> processedUsers = new ArrayList<>();

        // 현재 활성 사용자 수 확인
        Long activeUsers = redisTemplate.opsForValue().increment(ACTIVE_USERS_KEY, 0);
        int availableSlots = MAX_CONCURRENT_USERS - activeUsers.intValue();

        if (availableSlots <= 0) {
            return processedUsers; // 여유 슬롯 없음
        }

        // 처리할 배치 크기 계산
        int adjustedBatchSize = Math.min(batchSize, availableSlots);

        // 대기열에서 배치 처리
        for (int i = 0; i < adjustedBatchSize; i++) {
            String userId = redisTemplate.opsForList().leftPop(WAITING_QUEUE_KEY);

            // 활성 세션 등록
            redisTemplate.opsForValue().increment(ACTIVE_USERS_KEY);
            redisTemplate.opsForValue().set(SESSION_PREFIX + userId, "active", SESSION_TIMEOUT);
            processedUsers.add(userId);

            // 사용자에게 접속 허용 알림
            sseEmitterService.sendQueueStatus(userId, new QueueStatusDto(true, 0, 0));
            log.info("사용자 {} 접속 허용", userId);
        }

        // 남은 대기열 사용자들에게 상태 업데이트 전송
        List<String> remainingUsers = redisTemplate.opsForList().range(WAITING_QUEUE_KEY, 0, -1);
        if (!remainingUsers.isEmpty()) {
            for (int i = 0; i < remainingUsers.size(); i++) {
                String userId = remainingUsers.get(i);
                int position = i + 1;
                int waitTime = position * 5;
                sseEmitterService.sendQueueStatus(userId, new QueueStatusDto(false, position, waitTime));
            }
            log.info("{} 명의 사용자 대기 중", remainingUsers.size());
        }

        return processedUsers;
    }

    /**
     * 사용자 세션 종료
     */
    public void releaseSession(String userId) {
        Boolean hasSession = redisTemplate.hasKey(SESSION_PREFIX + userId);
        if (Boolean.TRUE.equals(hasSession)) {
            redisTemplate.delete(SESSION_PREFIX + userId);
            redisTemplate.opsForValue().decrement(ACTIVE_USERS_KEY);
            log.info("사용자 {} 세션 종료", userId);
        }
    }

    /**
     * 대기열에 사용자가 있는지 확인
     */
    private boolean isUserInQueue(String userId) {
        return getPositionInQueue(userId) > 0;
    }

    /**
     * 대기열에서 사용자 위치 확인
     */
    private int getPositionInQueue(String userId) {
        List<String> queueMembers = redisTemplate.opsForList().range(WAITING_QUEUE_KEY, 0, -1);

        for (int i = 0; i < queueMembers.size(); i++) {
            if (userId.equals(queueMembers.get(i))) {
                return i + 1; // 1부터 시작하는 위치 반환
            }
        }

        return 0; // 대기열에 없음
    }
}
