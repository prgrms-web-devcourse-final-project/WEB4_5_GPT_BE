package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.QueueStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
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
     * 연결 유지를 위한 하트비트 전송
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
