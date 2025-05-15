package com.WEB4_5_GPT_BE.unihub.domain.notice.scheduler;

import com.WEB4_5_GPT_BE.unihub.domain.notice.entity.Notice;
import com.WEB4_5_GPT_BE.unihub.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticePurgeScheduler {

    private final NoticeRepository noticeRepository;

    /**
     * 매일 새벽 3시에 30일 이전에 소프트 삭제된 공지사항을 영구 삭제합니다.
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void purgeSoftDeletedNotices() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<Notice> oldDeleted = noticeRepository.findAllByIsDeletedTrueAndModifiedAtBefore(cutoff);
        if (!oldDeleted.isEmpty()) {
            noticeRepository.deleteAllInBatch(oldDeleted);
            log.info("Purged {} old soft-deleted notices", oldDeleted.size());
        }
    }
}
