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
import com.WEB4_5_GPT_BE.unihub.global.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;
    private final S3Service s3Service;

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
    public NoticeCreateResponse createNotice(Long memberId, NoticeCreateRequest request, MultipartFile file) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UnihubException("404", "회원 정보를 찾을 수 없습니다."));
        if (member.getRole() != Role.ADMIN) {
            throw new UnihubException("403", "관리자만 공지사항을 작성할 수 있습니다.");
        }

        String url = null;
        try {
            if (file != null && !file.isEmpty()) {
                url = s3Service.upload(file);
            }

            Notice notice = Notice.builder()
                    .title(request.title())
                    .content(request.content())
                    .attachmentUrl(url)
                    .build();

            noticeRepository.save(notice);
            return NoticeCreateResponse.from(notice);
        } catch (IOException e) {
            throw new UnihubException("500", "파일 업로드에 실패했습니다.");
        }
    }

    // 수정
    public NoticeUpdateResponse updateNotice(Long memberId, Long id, NoticeUpdateRequest request, MultipartFile file) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UnihubException("404", "회원 정보를 찾을 수 없습니다."));
        if (member.getRole() != Role.ADMIN) {
            throw new UnihubException("403", "관리자만 공지사항을 수정할 수 있습니다.");
        }

        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UnihubException("404", "존재하지 않는 공지사항입니다."));

        String oldUrl = notice.getAttachmentUrl();
        String newUrl = oldUrl;

        try {
            if (file != null && !file.isEmpty()) {
                newUrl = s3Service.upload(file);
            }

            notice.update(request.title(), request.content(), newUrl);

            // 새 파일이 업로드된 경우 기존 파일 삭제
            if (file != null && !file.isEmpty() && oldUrl != null && !oldUrl.equals(newUrl)) {
                s3Service.deleteByUrl(oldUrl);
            }

            return NoticeUpdateResponse.from(notice);
        } catch (IOException e) {
            // 업로드 실패한 새 파일이 있다면 롤백
            if (!newUrl.equals(oldUrl) && newUrl != null) {
                s3Service.deleteByUrl(newUrl);
            }
            throw new UnihubException("500", "파일 업로드에 실패했습니다.");
        }
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

        // S3 파일 삭제 시도 (실패해도 계속 진행)
        String url = notice.getAttachmentUrl();
        if (url != null) {
            try {
                s3Service.deleteByUrl(url);
            } catch (Exception e) {
                log.warn("S3 파일 삭제 실패 (무시): {}", url, e);
            }
        }

        notice.softDelete();
        return NoticeDeleteResponse.from("공지사항이 삭제되었습니다.");
    }
}
