package com.WEB4_5_GPT_BE.unihub.domain.notice.service;

import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request.NoticeCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request.NoticeUpdateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.response.*;
import com.WEB4_5_GPT_BE.unihub.domain.notice.entity.Notice;
import com.WEB4_5_GPT_BE.unihub.domain.notice.repository.NoticeRepository;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;

    // 목록 조회
    @Transactional(readOnly = true)
    public List<NoticeListResponse> getNotices(String title) {
        List<Notice> notices = (title == null || title.isBlank())
                ? noticeRepository.findByIsDeletedFalseOrderByCreatedAtDesc()
                : noticeRepository.findByTitleContainingAndIsDeletedFalseOrderByCreatedAtDesc(title);
        return notices.stream().map(NoticeListResponse::from).toList();
    }

    // 상세 조회
    @Transactional(readOnly = true)
    public NoticeDetailResponse getNotice(Long id) {
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UnihubException("404", "존재하지 않는 공지사항입니다."));
        return NoticeDetailResponse.from(notice);
    }

    // 생성
    public NoticeCreateResponse createNotice(NoticeCreateRequest request) {
        Notice notice = Notice.builder()
                .title(request.title())
                .content(request.content())
                .attachmentUrl(request.attachmentUrl())
                .build();
        noticeRepository.save(notice);
        return NoticeCreateResponse.from(notice);
    }

    // 수정
    public NoticeUpdateResponse updateNotice(Long id, NoticeUpdateRequest request) {
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UnihubException("404", "존재하지 않는 공지사항입니다."));
        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setAttachmentUrl(request.attachmentUrl());
        return NoticeUpdateResponse.from(notice);
    }

    // 삭제
    public NoticeDeleteResponse deleteNotice(Long id) {
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UnihubException("404", "존재하지 않는 공지사항입니다."));
        notice.softDelete();
        return NoticeDeleteResponse.from("공지사항이 삭제되었습니다.");
    }
}
