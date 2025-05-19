package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentCommandConsumer {

    private final EnrollmentCommandHandler processor;
    private final RBlockingQueue<EnrollmentCommand> enrollQueue;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread worker = new Thread(this::runLoop, "enroll-worker");
        worker.setDaemon(true);
        worker.start();
    }

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
                Thread.currentThread().interrupt();
            }
        }
    }
}