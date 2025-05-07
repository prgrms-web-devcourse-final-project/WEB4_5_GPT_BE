package com.WEB4_5_GPT_BE.unihub.domain.member.batch;

import com.WEB4_5_GPT_BE.unihub.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 학생들의 학기와 학년 정보를 자동으로 업데이트하는 스케줄러
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SemesterUpdateScheduler {

    private final MemberService memberService;

    /**
     * 1학기 시작 시점 (매년 3월 1일 오전 3시에 실행)
     * 2학기가 끝나고 다음 학년의 1학기로 업데이트
     */
    @Scheduled(cron = "0 0 3 1 3 *")
    public void updateSemesterForSpring() {
        try {
            log.info("1학기 시작 - 모든 학생의 학기 정보 업데이트 실행 - {}", LocalDateTime.now());
            memberService.updateAllStudentSemesters();
            log.info("학기 정보 업데이트 완료");
        } catch (Exception e) {
            log.error("1학기 업데이트 작업 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 2학기 시작 시점 (매년 9월 1일 오전 3시에 실행)
     * 1학기가 끝나고 2학기로 업데이트
     */
    @Scheduled(cron = "0 0 3 1 9 *")
    public void updateSemesterForFall() {
        try {
            log.info("2학기 시작 - 모든 학생의 학기 정보 업데이트 실행 - {}", LocalDateTime.now());
            memberService.updateAllStudentSemesters();
            log.info("학기 정보 업데이트 완료");
        } catch (Exception e) {
            log.error("2학기 업데이트 작업 실패: {}", e.getMessage(), e);
        }
    }
}
