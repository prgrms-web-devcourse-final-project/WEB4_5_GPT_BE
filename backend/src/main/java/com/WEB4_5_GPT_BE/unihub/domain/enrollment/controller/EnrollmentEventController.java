package com.WEB4_5_GPT_BE.unihub.domain.enrollment.controller;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.QueueStatusDto;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.EnrollmentQueueService;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.SseEmitterService;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import com.WEB4_5_GPT_BE.unihub.global.response.Empty;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/enrollment")
@RequiredArgsConstructor
public class EnrollmentEventController {

    private final SseEmitterService sseEmitterService;
    private final EnrollmentQueueService enrollmentQueueService;

    /**
     * SSE 연결 엔드포인트
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToEvents(SecurityUser securityUser) {
        // 인증된 사용자 정보가 없는 경우 예외 처리
        if (securityUser == null) {
            log.warn("인증되지 않은 SSE 연결 시도");
            throw new UnihubException("401", "인증되지 않은 요청입니다.");
        }

        Long memberId = securityUser.getId();
        log.info("SSE 연결 요청: {}", memberId);

        // 초기 상태를 포함한 SSE 이미터 생성 (서비스로 기능 이동)
        return sseEmitterService.createEmitterWithInitialStatus(String.valueOf(memberId));
    }

    /**
     * 대기열에 사용자 추가 엔드포인트
     */
    @PostMapping("/queue/join")
    public RsData<QueueStatusDto> joinQueue(SecurityUser securityUser) {
        if (securityUser == null) {
            throw new UnihubException("401", "인증되지 않은 요청입니다.");
        }

        QueueStatusDto status = enrollmentQueueService.addToQueue(String.valueOf(securityUser.getId()));
        return new RsData<>("200", "대기열 참여 요청 성공", status);
    }

    /**
     * 현재 사용자의 대기열 상태 조회 엔드포인트
     */
    @GetMapping("/queue/status")
    public RsData<QueueStatusDto> getQueueStatus(SecurityUser securityUser) {
        if (securityUser == null) {
            throw new UnihubException("401", "인증되지 않은 요청입니다.");
        }

        QueueStatusDto status = enrollmentQueueService.getQueueStatus(String.valueOf(securityUser.getId()));
        return new RsData<>("200", "대기열 상태 조회 성공", status);
    }

    /**
     * 사용자 세션 종료 엔드포인트 (로그아웃 시 호출)
     */
    @PostMapping("/queue/release")
    public RsData<Empty> releaseSession(SecurityUser securityUser) {
        if (securityUser == null) {
            throw new UnihubException("401", "인증되지 않은 요청입니다.");
        }

        enrollmentQueueService.releaseSession(String.valueOf(securityUser.getId()));
        return new RsData<>("200", "세션이 정상적으로 종료되었습니다.");
    }
}
