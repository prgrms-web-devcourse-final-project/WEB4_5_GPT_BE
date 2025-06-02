package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.cancel;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 수강취소 명령 소비자 컴포넌트
 * <p>
 * Redis의 Queue(RBlockingQueue)에서 수강취소 요청(EnrollmentCancelCommand)을 읽어와
 * 수강취소 명령 처리 핸들러(EnrollmentCancelCommandHandler)를 통해 비동기로 동작합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentCancelCommandConsumer {

    // 수강취소 처리 로직이 구현된 핸들러
    private final EnrollmentCancelCommandHandler handler;

    // Redis 기반 큐 (수강취소 명령 대기열)
    private final RBlockingQueue<EnrollmentCancelCommand> cancelQueue;

    /**
     * 워커 스레드를 보관해두기 위한 필드
     */
    private Thread worker;

    /**
     * 애플리케이션이 완전히 시작된 후 호출되어
     * 워커 스레드를 생성·시작합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        worker = new Thread(this::runLoop, "cancel-worker");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 스프링 컨텍스트 종료 시 호출되어 워커를 안전히 중단합니다.
     */
    @PreDestroy
    public void stop() {
        if (worker != null && worker.isAlive()) {
            log.info("EnrollmentCancelCommandConsumer 종료중...");
            worker.interrupt();
        }
    }

    /**
     * 워커 스레드 실행 메서드
     * <p>
     * - 큐에 명령이 들어올 때까지 블로킹하여 기다립니다.
     * - 꺼낸 명령은 handler.process()로 처리합니다.
     * - 처리 중 문제가 생기면 경고 로그를 남깁니다.
     * - RBlockingQueue는 Redis 쪽에서 블로킹 호출을 제공하므로,
     * 메시지가 없을 때는 CPU를 소모하지 않고 메시지 도착 시까지 대기합니다.
     */
    private void runLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                EnrollmentCancelCommand cmd = cancelQueue.take();
                try {
                    handler.process(cmd);
                } catch (Exception ex) {
                    log.warn("수강취소 처리 중 오류: 학생={} 강의={} 예외={}",
                            cmd.studentId(), cmd.courseId(), ex.toString());
                }
            } catch (InterruptedException ie) {
                // 인터럽트 발생: 다른 스레드가 이 스레드의 종료를 요청
                // 스레드의 인터럽트 상태를 다시 설정하고 반복을 종료합니다.
                Thread.currentThread().interrupt();
            }
        }
    }
}