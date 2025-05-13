package com.WEB4_5_GPT_BE.unihub.domain.notice.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request.NoticeCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request.NoticeUpdateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.response.*;
import com.WEB4_5_GPT_BE.unihub.domain.notice.entity.Notice;
import com.WEB4_5_GPT_BE.unihub.domain.notice.repository.NoticeRepository;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;

    // 목록 조회
    @Transactional(readOnly = true)
    public Page<NoticeListResponse> getNotices(String title, Pageable pageable) {
        Page<Notice> notices = (title == null || title.isBlank())
                ? noticeRepository.findByIsDeletedFalse(pageable)
                : noticeRepository.findByTitleContainingAndIsDeletedFalse(title, pageable);
        return notices.map(NoticeListResponse::from);
    }

    // 상세 조회
    @Transactional(readOnly = true)
    public NoticeDetailResponse getNotice(Long id) {
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UnihubException("404", "존재하지 않는 공지사항입니다."));
        return NoticeDetailResponse.from(notice);
    }

    // 생성
    public NoticeCreateResponse createNotice(Long memberId, NoticeCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UnihubException("404", "회원 정보를 찾을 수 없습니다."));
        if (member.getRole() != Role.ADMIN) {
            throw new UnihubException("403", "관리자만 공지사항을 작성할 수 있습니다.");
        }

        Notice notice = Notice.builder()
                .title(request.title())
                .content(request.content())
                .attachmentUrl(request.attachmentUrl())
                .build();
        noticeRepository.save(notice);
        return NoticeCreateResponse.from(notice);
    }

    // 수정
    public NoticeUpdateResponse updateNotice(Long memberId, Long id, NoticeUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UnihubException("404", "회원 정보를 찾을 수 없습니다."));
        if (member.getRole() != Role.ADMIN) {
            throw new UnihubException("403", "관리자만 공지사항을 수정할 수 있습니다.");
        }

        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UnihubException("404", "존재하지 않는 공지사항입니다."));

        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setAttachmentUrl(request.attachmentUrl());
        return NoticeUpdateResponse.from(notice);
    }
    // 삭제
    public NoticeDeleteResponse deleteNotice(Long memberId, Long id) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UnihubException("404", "회원 정보를 찾을 수 없습니다."));
        if (member.getRole() != Role.ADMIN) {
            throw new UnihubException("403", "관리자만 공지사항을 삭제할 수 있습니다.");
        }

        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UnihubException("404", "존재하지 않는 공지사항입니다."));
        notice.softDelete();
        return NoticeDeleteResponse.from("공지사항이 삭제되었습니다.");
    }
}
