package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.QueueStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

    // 타임아웃 설정 (3분)
    private static final long TIMEOUT = 180000L;
    // 사용자 ID별 SSE 이미터를 관리하는 맵
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 새로운 SSE 연결 생성
     */
    public SseEmitter createEmitter(String memberId) {
        // 기존 연결이 있으면 완료 처리
        removeEmitterIfExists(memberId);

        // 새로운 이미터 생성
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        // 이벤트 핸들러 등록
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: {}", memberId);
            removeEmitterIfExists(memberId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: {}", memberId);
            removeEmitterIfExists(memberId);
        });

        emitter.onError(e -> {
            log.error("SSE 연결 오류: {}, 에러: {}", memberId, e.getMessage());
            removeEmitterIfExists(memberId);
        });

        // 이미터 저장
        emitters.put(memberId, emitter);
        log.info("새로운 SSE 연결 생성: {}", memberId);

        // 연결 유지를 위한 초기 이벤트 전송
        sendHeartbeat(emitter);

        return emitter;
    }

    /**
     * 특정 사용자에게 대기열 상태 업데이트 전송
     */
    public void sendQueueStatus(String memberId, QueueStatusDto status) {
        SseEmitter emitter = emitters.get(memberId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("QUEUE_STATUS")
                        .data(status));
                log.debug("대기열 상태 전송: {}, 상태: {}", memberId, status);
            } catch (IOException e) {
                log.error("이벤트 전송 실패: {}", memberId, e);
                removeEmitterIfExists(memberId);
            }
        }
    }

    /**
     * 활성 SSE 연결 수 조회
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }

    /**
     * 사용자의 현재 대기열 상태를 초기화하는 이미터 생성
     *
     * @param memberId      사용자 ID
     * @param initialStatus 초기 상태 정보
     * @return 초기화된 SSE 이미터
     */
    public SseEmitter createEmitterWithInitialStatus(String memberId, QueueStatusDto initialStatus) {
        // 기본 이미터 생성
        SseEmitter emitter = createEmitter(memberId);

        // 초기 상태 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("INITIAL_STATUS")
                    .data(initialStatus));
            log.info("초기 상태 전송: {}", memberId);
        } catch (Exception e) {
            log.error("초기 상태 전송 실패: {}", memberId, e);
            removeEmitterIfExists(memberId);
        }

        return emitter;
    }

    /**
     * 연결 유지를 위한 하트빃 전송
     */
    private void sendHeartbeat(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("HEARTBEAT")
                    .data(""));
        } catch (IOException e) {
            log.error("하트비트 전송 실패", e);
        }
    }

    /**
     * 기존 이미터 제거
     */
    private void removeEmitterIfExists(String memberId) {
        SseEmitter emitter = emitters.remove(memberId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("이미터 완료 처리 실패: {}", memberId, e);
            }
        }
    }
}
