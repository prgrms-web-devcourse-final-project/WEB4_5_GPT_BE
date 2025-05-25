package com.WEB4_5_GPT_BE.unihub.domain.enrollment.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.QueueStatusDto;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.EnrollmentQueueService;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import com.WEB4_5_GPT_BE.unihub.global.response.Empty;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Enrollment", description = "수강신청 관련 API (대기열 관리, 이벤트 스트림 등)")
@Slf4j
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentEventController {

    private final EnrollmentQueueService enrollmentQueueService;

    // SSE 관련 코드 제거

    @Operation(summary = "대기열 참여", description = "수강신청 대기열에 사용자를 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "대기열 참여 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/queue/join")
    public RsData<QueueStatusDto> joinQueue(@AuthenticationPrincipal SecurityUser securityUser) {
        if (securityUser == null) {
            throw new UnihubException("401", "인증되지 않은 요청입니다.");
        }

        QueueStatusDto status = enrollmentQueueService.addToQueue(String.valueOf(securityUser.getId()));
        return new RsData<>("200", "대기열 참여 요청 성공", status);
    }

    @Operation(summary = "대기열 상태 조회", description = "현재 사용자의 대기열 상태를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/queue/status")
    public RsData<QueueStatusDto> getQueueStatus(@AuthenticationPrincipal SecurityUser securityUser) {
        if (securityUser == null) {
            throw new UnihubException("401", "인증되지 않은 요청입니다.");
        }

        QueueStatusDto status = enrollmentQueueService.getQueueStatus(String.valueOf(securityUser.getId()));
        return new RsData<>("200", "대기열 상태 조회 성공", status);
    }

    @Operation(summary = "세션 종료", description = "사용자 세션을 종료하고 대기열에서 제거합니다 (로그아웃 시 호출).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "세션 종료 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/queue/release")
    public RsData<Empty> releaseSession(@AuthenticationPrincipal SecurityUser securityUser) {
        if (securityUser == null) {
            throw new UnihubException("401", "인증되지 않은 요청입니다.");
        }

        enrollmentQueueService.releaseSession(String.valueOf(securityUser.getId()));
        return new RsData<>("200", "세션이 정상적으로 종료되었습니다.");
    }
}
