package com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatusDto {
    private boolean allowed;     // 접속 허용 여부
    private int position;        // 대기열 위치 (1부터 시작)
    private int estimatedWaitTime; // 예상 대기 시간(초)
    private String message;      // 상태 메시지

    public QueueStatusDto(boolean allowed, int position, int estimatedWaitTime) {
        this.allowed = allowed;
        this.position = position;
        this.estimatedWaitTime = estimatedWaitTime;
        this.message = allowed
                ? "수강신청 페이지에 접속이 허용되었습니다."
                : String.format("현재 대기열 %d번 위치에 있습니다. 예상 대기 시간: %d초", position, estimatedWaitTime);
    }
}
