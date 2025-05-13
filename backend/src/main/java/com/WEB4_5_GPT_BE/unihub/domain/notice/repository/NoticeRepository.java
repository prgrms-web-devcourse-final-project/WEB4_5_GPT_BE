package com.WEB4_5_GPT_BE.unihub.domain.notice.repository;

import com.WEB4_5_GPT_BE.unihub.domain.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 공지사항 목록 조회 (삭제되지 않은 공지사항 전체, 최신순)
    Page<Notice> findByIsDeletedFalse(Pageable pageable);

    // 공지사항 목록 조회 (제목 검색 포함)
    Page<Notice> findByTitleContainingAndIsDeletedFalse(String title, Pageable pageable);

    // 상세 조회 (삭제되지 않은 것만)
    Optional<Notice> findByIdAndIsDeletedFalse(Long id);
}
