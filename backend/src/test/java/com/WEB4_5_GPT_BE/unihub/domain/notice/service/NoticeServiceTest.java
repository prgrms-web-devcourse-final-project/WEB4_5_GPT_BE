package com.WEB4_5_GPT_BE.unihub.domain.notice.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Admin;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request.NoticeCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request.NoticeUpdateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.response.*;
import com.WEB4_5_GPT_BE.unihub.domain.notice.entity.Notice;
import com.WEB4_5_GPT_BE.unihub.domain.notice.repository.NoticeRepository;
import com.WEB4_5_GPT_BE.unihub.domain.notice.service.NoticeService;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import com.WEB4_5_GPT_BE.unihub.global.infra.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private NoticeService noticeService;

    private Member admin;
    private Member student;

    @BeforeEach
    void setUp() {
        admin = Admin.builder().id(1L).email("admin@auni.ac.kr").role(Role.ADMIN).build();
        student = Student.builder().id(2L).email("student@auni.ac.kr").role(Role.STUDENT).build();
    }

    @Test
    @DisplayName("공지사항 생성 성공 - 파일 없음")
    void createNotice_success() {
        NoticeCreateRequest request = new NoticeCreateRequest("제목", "내용", null);
        when(memberRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        NoticeCreateResponse response = noticeService.createNotice(admin.getId(), request, null);

        assertThat(response.title()).isEqualTo("제목");
        verify(noticeRepository).save(any(Notice.class));
    }
    @Test
    @DisplayName("공지사항 생성 성공 - 파일 포함")
    void createNotice_withFile_success() throws Exception {
        NoticeCreateRequest request = new NoticeCreateRequest("제목", "내용", null);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes()
        );

        when(memberRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(s3Service.upload(file)).thenReturn("https://bucket/test.pdf");

        NoticeCreateResponse response = noticeService.createNotice(admin.getId(), request, file);

        assertThat(response.attachmentUrl()).isEqualTo("https://bucket/test.pdf");
        verify(noticeRepository).save(any(Notice.class));
    }

    @Test
    @DisplayName("공지사항 생성 실패 - 관리자 권한 아님")
    void createNotice_forbidden() {
        NoticeCreateRequest request = new NoticeCreateRequest("제목", "내용", null);
        when(memberRepository.findById(student.getId())).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> noticeService.createNotice(student.getId(), request, null))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("관리자만 공지사항을 작성할 수 있습니다.");
    }

    @Test
    @DisplayName("공지사항 단건 조회 성공")
    void getNotice_success() {
        Notice notice = Notice.builder().id(1L).title("제목").content("내용").build();
        when(noticeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(notice));

        NoticeDetailResponse response = noticeService.getNotice(1L);
        assertThat(response.title()).isEqualTo("제목");
    }

    @Test
    @DisplayName("공지사항 단건 조회 실패 - 존재하지 않음")
    void getNotice_fail_notFound() {
        when(noticeRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.getNotice(999L))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("존재하지 않는 공지사항");
    }

    @Test
    @DisplayName("공지사항 수정 성공 - 파일 없음")
    void updateNotice_success() {
        NoticeUpdateRequest request = new NoticeUpdateRequest("수정된 제목", "수정된 내용", null);
        Notice notice = Notice.builder().id(1L).title("기존 제목").content("기존 내용").build();

        when(memberRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(noticeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(notice));

        NoticeUpdateResponse response = noticeService.updateNotice(admin.getId(), 1L, request, null);

        assertThat(response.title()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("공지사항 수정 성공 - 새 파일 포함")
    void updateNotice_withFile_success() throws Exception {
        NoticeUpdateRequest request = new NoticeUpdateRequest("수정제목", "수정내용", null);
        Notice notice = Notice.builder()
                .id(1L)
                .title("기존 제목")
                .content("기존 내용")
                .attachmentUrl("https://bucket/old.pdf")
                .build();
        MockMultipartFile newFile = new MockMultipartFile("file", "new.pdf", "application/pdf", "new content".getBytes());

        when(memberRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(noticeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(notice));
        when(s3Service.upload(newFile)).thenReturn("https://bucket/new.pdf");

        NoticeUpdateResponse response = noticeService.updateNotice(admin.getId(), 1L, request, newFile);

        assertThat(response.attachmentUrl()).isEqualTo("https://bucket/new.pdf");
        verify(s3Service).deleteByUrl("https://bucket/old.pdf");
    }

    @Test
    @DisplayName("공지사항 삭제 성공 - 첨부파일 포함")
    void deleteNotice_success() {
        Notice notice = Notice.builder()
                .id(1L)
                .title("삭제할 공지")
                .attachmentUrl("https://bucket/to-delete.pdf")
                .build();

        when(memberRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(noticeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(notice));

        NoticeDeleteResponse response = noticeService.deleteNotice(admin.getId(), 1L);

        assertThat(response.message()).isEqualTo("공지사항이 삭제되었습니다.");
        assertThat(notice.isDeleted()).isTrue();
        verify(s3Service).deleteByUrl("https://bucket/to-delete.pdf");
    }

    @Test
    @DisplayName("공지사항 목록 조회 성공")
    void getNotices_success() {
        // given
        Notice notice1 = Notice.builder().id(1L).title("제목1").content("내용1").build();
        Notice notice2 = Notice.builder().id(2L).title("제목2").content("내용2").build();

        Page<Notice> page = new PageImpl<>(List.of(notice1, notice2));

        when(noticeRepository.findByTitleContainingAndIsDeletedFalse(eq("제목"), any(Pageable.class)))
                .thenReturn(page);

        // when
        Page<NoticeListResponse> result = noticeService.getNotices("제목", PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).title()).isEqualTo("제목1");
    }
}
