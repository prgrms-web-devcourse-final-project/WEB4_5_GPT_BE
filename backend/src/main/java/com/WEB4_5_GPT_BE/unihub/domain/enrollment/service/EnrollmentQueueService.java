package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.QueueStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentQueueService {

    private static final String WAITING_QUEUE_KEY = "enrollment:waiting-queue";
    private static final String SESSION_PREFIX = "enrollment:session:";
    private static final int MAX_CONCURRENT_USERS = 3;
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(10);
    private final StringRedisTemplate redisTemplate;
    private final SseEmitterService sseEmitterService;
    // 대기열 상태 메시지 전송 주기 (5초)
    private static final long QUEUE_MESSAGE_INTERVAL = 5000;
    // 업데이트 오프셋 (일정 수 이상 변경이 있을 때 업데이트)
    private static final int QUEUE_MESSAGE_BATCH_SIZE = 5;
    // 마지막으로 대기열 상태 메시지를 전송한 시간
    private long lastQueueMessageTime = 0;
    // 최근 처리된 사용자 수 (배치 처리용)
    private int processedUserCount = 0;

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
            int waitTime = position * 60 * 5; // 사용자당 5분 소요 가정
            QueueStatusDto status = new QueueStatusDto(false, position, waitTime);
            sseEmitterService.sendQueueStatus(memberId, status);
            return status;
        }

        // 현재 활성 사용자 수 확인
        Long activeUsers = getActiveUserCount();

        // 여유 자리가 있으면 바로 접속 허용
        if (activeUsers < MAX_CONCURRENT_USERS) {
            redisTemplate.opsForValue().set(SESSION_PREFIX + memberId, "active", SESSION_TIMEOUT);
            log.info("사용자 {} 즉시 접속 허용", memberId);

            QueueStatusDto status = new QueueStatusDto(true, 0, 0);
            sseEmitterService.sendQueueStatus(memberId, status);
            return status;
        }

        // 대기열에 추가
        redisTemplate.opsForList().rightPush(WAITING_QUEUE_KEY, memberId);
        int position = getPositionInQueue(memberId);
        int waitTime = position * 60 * 5; // 사용자당 5분 소요 가정

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
            log.info("사용자 {} 세션 종료", memberId);

            // 세션 종료 후 대기열에서 다음 사용자 처리
            processNextInQueue();
        }
    }

    /**
     * 대기열에서 다음 사용자 처리 - Redis 대기열은 실시간으로 업데이트
     * 활성 접속 공간이 있을 때 대기열에서 가장 앞에 있는 사용자를 가져와 접속 허용
     */
    public void processNextInQueue() {
        // 활성 사용자 수 확인
        Long activeUsers = getActiveUserCount();

        // 여유 자리가 있고 대기열에 사용자가 있는 경우
        if (activeUsers < MAX_CONCURRENT_USERS) {
            // 대기열에서 다음 사용자 가져오기 (leftPop 사용) - Redis 대기열 실시간 업데이트
            String nextMemberId = redisTemplate.opsForList().leftPop(WAITING_QUEUE_KEY);

            // 세션 활성화
            redisTemplate.opsForValue().set(SESSION_PREFIX + nextMemberId, "active", SESSION_TIMEOUT);

            // 사용자에게 접속 허용 알림 (SSE 이벤트 전송)
            QueueStatusDto status = new QueueStatusDto(true, 0, 0);
            sseEmitterService.sendQueueStatus(nextMemberId, status);
            log.info("사용자 {} 대기열에서 접속 허용", nextMemberId);

            // 처리된 사용자 카운트 증가
            processedUserCount++;

            // 대기열 상태 메시지 배치 전송 조건 확인
            long currentTime = System.currentTimeMillis();
            boolean timeBasedUpdate = (currentTime - lastQueueMessageTime) > QUEUE_MESSAGE_INTERVAL;
            boolean countBasedUpdate = processedUserCount >= QUEUE_MESSAGE_BATCH_SIZE;

            if (timeBasedUpdate || countBasedUpdate) {
                // 대기열 상태 메시지만 배치로 전송 (Redis 대기열 업데이트는 이미 위에서 실시간으로 이루어짐)
                sendQueueStatusMessages();
                lastQueueMessageTime = currentTime;
                processedUserCount = 0;
                log.info("대기열 사용자에게 상태 메시지 전송 - {} 기준", timeBasedUpdate ? "시간" : "처리량");
            }
        }
    }

    /**
     * 대기열에 있는 모든 사용자에게 상태 메시지만 전송 (Redis 데이터 업데이트 없이 메시지만 전송)
     */
    private void sendQueueStatusMessages() {
        // 현재 대기열 상태 조회 (Redis 대기열은 변경하지 않고 읽기만 함)
        List<String> queueMembers = redisTemplate.opsForList().range(WAITING_QUEUE_KEY, 0, -1);

        // 대기열에 있는 모든 사용자에게 상태 메시지 전송
        for (int i = 0; i < queueMembers.size(); i++) {
            String memberId = queueMembers.get(i);
            int position = i + 1; // 1부터 시작하는 위치
            int waitTime = position * 30; // 사용자당 30초 소요 가정

            QueueStatusDto status = new QueueStatusDto(false, position, waitTime);
            sseEmitterService.sendQueueStatus(memberId, status);
        }
    }

    /**
     * 대기열에 사용자가 있는지 확인
     */
    public boolean isUserInQueue(String memberId) {
        return getPositionInQueue(memberId) > 0;
    }

    /**
     * 대기열에서 사용자 위치 확인
     */
    public int getPositionInQueue(String memberId) {
        List<String> queueMembers = redisTemplate.opsForList().range(WAITING_QUEUE_KEY, 0, -1);

        for (int i = 0; i < queueMembers.size(); i++) {
            if (memberId.equals(queueMembers.get(i))) {
                return i + 1; // 1부터 시작하는 위치 반환
            }
        }

        return 0; // 대기열에 없음
    }

    /**
     * 현재 활성 사용자 수 계산 (Redis SCAN 명령어 사용)
     */
    protected Long getActiveUserCount() {
        long count = 0;
        ScanOptions options = ScanOptions.scanOptions().match(SESSION_PREFIX + "*").build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                cursor.next();
                count++;
            }
        }

        return count;
    }
}
