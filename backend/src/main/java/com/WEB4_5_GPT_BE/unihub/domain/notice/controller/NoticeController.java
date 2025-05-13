package com.WEB4_5_GPT_BE.unihub.domain.notice.controller;

import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request.NoticeCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request.NoticeUpdateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.response.*;
import com.WEB4_5_GPT_BE.unihub.domain.notice.service.NoticeService;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지사항 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공지사항 목록 조회 성공")
    })
    @GetMapping
    public RsData<Page<NoticeListResponse>> getNotices(
            @RequestParam(value = "title", required = false) String title,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return new RsData<>("200", "공지사항 목록 조회 성공", noticeService.getNotices(title, pageable));
    }

    @Operation(summary = "공지사항 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공지사항 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 공지사항입니다.")
    })
    @GetMapping("/{id}")
    public RsData<NoticeDetailResponse> getNotice(@PathVariable Long id) {
        return new RsData<>("200", "공지사항 상세 조회 성공", noticeService.getNotice(id));
    }

    @Operation(summary = "공지사항 작성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공지사항 작성 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 누락 또는 첨부파일 업로드 실패")
    })
    @PostMapping
    public RsData<NoticeCreateResponse> createNotice(
            @AuthenticationPrincipal SecurityUser user,
            @RequestBody NoticeCreateRequest request
    ) {
        return new RsData<>("200", "공지사항 작성 성공", noticeService.createNotice(user.getId(), request));
    }

    @Operation(summary = "공지사항 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공지사항 수정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 공지사항입니다.")
    })
    @PatchMapping("/{id}")
    public RsData<NoticeUpdateResponse> updateNotice(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @RequestBody NoticeUpdateRequest request
    ) {
        return new RsData<>("200", "공지사항 수정 성공", noticeService.updateNotice(user.getId(), id, request));
    }

    @Operation(summary = "공지사항 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공지사항 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 공지사항입니다.")
    })
    @DeleteMapping("/{id}")
    public RsData<NoticeDeleteResponse> deleteNotice(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id
    ) {
        return new RsData<>("200", "공지사항 삭제 성공", noticeService.deleteNotice(user.getId(), id));
    }
}
