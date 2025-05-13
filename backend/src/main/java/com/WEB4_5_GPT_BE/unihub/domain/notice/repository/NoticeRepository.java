package com.WEB4_5_GPT_BE.unihub.domain.notice.repository;

import com.WEB4_5_GPT_BE.unihub.domain.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 삭제되지 않은 공지사항 페이징 조회
    Page<Notice> findByIsDeletedFalse(Pageable pageable);

    // 제목에 키워드가 포함된 공지사항 페이징 조회 (삭제되지 않은 것만)
    Page<Notice> findByIsDeletedFalseAndTitleContainingIgnoreCase(String keyword, Pageable pageable);
}
