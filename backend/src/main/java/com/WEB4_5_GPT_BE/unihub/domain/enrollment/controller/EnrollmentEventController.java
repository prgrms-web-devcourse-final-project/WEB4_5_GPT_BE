package com.WEB4_5_GPT_BE.unihub.domain.enrollment.controller;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.QueueStatusDto;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/enrollment")
@RequiredArgsConstructor
public class EnrollmentEventController {

    private final SseEmitterService sseEmitterService;

    /**
     * SSE 연결 엔드포인트
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToEvents(Principal principal) {
        // 인증된 사용자 정보가 없는 경우 예외 처리
        if (principal == null) {
            log.warn("인증되지 않은 SSE 연결 시도");
            throw new RuntimeException("인증되지 않은 요청입니다.");
        }

        String userId = principal.getName();
        log.info("SSE 연결 요청: {}", userId);

        // SSE 이미터 생성
        SseEmitter emitter = sseEmitterService.createEmitter(userId);

        // 초기 상태 전송 - 현재는 기본 상태만 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("INITIAL_STATUS")
                    .data(new QueueStatusDto(false, 0, 0, "연결되었습니다. 대기열 정보를 기다리는 중...")));
            log.info("초기 상태 전송: {}", userId);
        } catch (Exception e) {
            log.error("초기 상태 전송 실패: {}", userId, e);
        }

        return emitter;
    }
}
