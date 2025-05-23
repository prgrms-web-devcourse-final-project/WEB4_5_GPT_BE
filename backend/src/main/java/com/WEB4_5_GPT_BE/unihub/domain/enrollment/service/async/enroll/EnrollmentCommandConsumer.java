package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.enroll;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;


/**
 * 수강신청 명령 소비자 컴포넌트
 * <p>
 * Redis의 Queue(RBlockingQueue)에서 수강신청 요청(EnrollmentCommand)을 읽어와
 * 수강신청 명령 처리 핸들러(EnrollmentCommandHandler)를 통해 비동기로 동작합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentCommandConsumer {

    // 수강신청 처리 로직이 구현된 핸들러
    private final EnrollmentCommandHandler processor;

    // Redis 기반 큐 (수강신청 명령 대기열)
    private final RBlockingQueue<EnrollmentCommand> enrollQueue;

    /**
     * 애플리케이션이 완전히 시작된 후 호출되어
     * 워커 스레드를 생성·시작합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread worker = new Thread(this::runLoop, "enroll-worker");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 워커 스레드 실행 메서드
     *
     * - 큐에 명령이 들어올 때까지 기다립니다.
     * - 꺼낸 명령은 processor.process()로 처리합니다.
     * - 처리 중 문제가 생기면 경고 로그를 남깁니다.
     * - RBlockingQueue의 경우 Redis 쪽에서 블로킹 호출이 됩니다. 즉 큐에 메시지가 없을 때는 CPU 를 소모하지 않고
     *   메시지가 들어올 때까지 블록 상태로 존재하여 부하가 적습니다.
     */
    private void runLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                EnrollmentCommand cmd = enrollQueue.take();
                try {
                    processor.process(cmd);
                } catch (Exception ex) {
                    log.warn("수강신청 처리 중 오류: 학생={} 강의={} 예외={}",
                            cmd.studentId(), cmd.courseId(), ex.toString());
                }
            } catch (InterruptedException ie) {
                // 인터럽트 발생: 다른 스레드가 이 스레드에 종료를 요청
                // 반복을 멈추고 스레드를 바로 종료합니다.
                Thread.currentThread().interrupt();
            }
        }
    }
}