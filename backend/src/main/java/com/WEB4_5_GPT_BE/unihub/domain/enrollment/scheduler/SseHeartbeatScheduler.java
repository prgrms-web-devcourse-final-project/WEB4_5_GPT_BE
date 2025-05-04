package com.WEB4_5_GPT_BE.unihub.domain.enrollment.scheduler;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

    private final SseEmitterService sseEmitterService;

    /**
     * 30초마다 모든 연결된 클라이언트에게 하트비트 전송
     * 이는 연결이 끊어지는 것을 방지하기 위함
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        int activeConnections = sseEmitterService.getActiveConnectionCount();
        if (activeConnections > 0) {
            log.debug("하트비트 전송, 현재 연결 수: {}", activeConnections);
        }
    }
}
