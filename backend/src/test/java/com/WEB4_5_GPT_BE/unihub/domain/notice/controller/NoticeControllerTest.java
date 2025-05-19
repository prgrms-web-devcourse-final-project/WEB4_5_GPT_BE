package com.WEB4_5_GPT_BE.unihub.domain.notice.controller;

import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.MemberLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request.NoticeCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request.NoticeUpdateRequest;
import com.WEB4_5_GPT_BE.unihub.global.config.RedisTestContainerConfig;
import com.WEB4_5_GPT_BE.unihub.global.infra.s3.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@RedisTestContainerConfig
class NoticeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;


    @MockBean
    private S3Service s3Service;

    @BeforeEach
    void setUp() throws Exception {
        // S3Service.upload 호출 시 고정된 URL 반환
        doReturn("https://team08-bucket-1.s3.ap-northeast-2.amazonaws.com/1747272581157_notice-summer.jpg")
                .when(s3Service).upload(any(MultipartFile.class));
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        var loginRequest = new MemberLoginRequest(email, password);
        String response = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("공지사항 목록 조회 성공")
    void getNotices_success() throws Exception {
        String token = loginAndGetAccessToken("adminmaster@auni.ac.kr", "adminPw");

        mockMvc.perform(get("/api/notices")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(greaterThan(0)));
    }

    @Test
    @DisplayName("공지사항 상세 조회 성공")
    void getNotice_success() throws Exception {
        String token = loginAndGetAccessToken("adminmaster@auni.ac.kr", "adminPw");

        mockMvc.perform(get("/api/notices/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("필독 공지"));
    }

    @Test
    @DisplayName("공지사항 작성 성공 - 파일 없이")
    void createNotice_success() throws Exception {
        String token = loginAndGetAccessToken("adminmaster@auni.ac.kr", "adminPw");

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", "application/json",
                objectMapper.writeValueAsBytes(new NoticeCreateRequest("새 공지", "공지 내용"))
        );

        mockMvc.perform(multipart("/api/notices")
                        .file(dataPart)
                        .header("Authorization", "Bearer " + token)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("새 공지"));
    }

    @Test
    @DisplayName("공지사항 작성 성공 - 파일 포함")
    void createNotice_withFile_success() throws Exception {
        String token = loginAndGetAccessToken("adminmaster@auni.ac.kr", "adminPw");

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", "application/json",
                objectMapper.writeValueAsBytes(new NoticeCreateRequest("공지제목", "공지내용"))
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "file", "sample.pdf", "application/pdf", "dummy content".getBytes()
        );

        mockMvc.perform(multipart("/api/notices")
                        .file(dataPart)
                        .file(filePart)
                        .header("Authorization", "Bearer " + token)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("공지제목"))
                .andExpect(jsonPath("$.data.attachmentUrl").value("https://team08-bucket-1.s3.ap-northeast-2.amazonaws.com/1747272581157_notice-summer.jpg"));
    }

    @Test
    @DisplayName("학생 권한으로 공지사항 작성 실패 - multipart")
    void createNotice_byStudent_thenForbidden() throws Exception {
        String token = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", "application/json",
                objectMapper.writeValueAsBytes(new NoticeCreateRequest("학생 공지", "학생이 작성"))
        );

        mockMvc.perform(multipart("/api/notices")
                        .file(dataPart)
                        .header("Authorization", "Bearer " + token)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("공지사항 수정 성공 - 파일 없이")
    void updateNotice_success() throws Exception {
        String token = loginAndGetAccessToken("adminmaster@auni.ac.kr", "adminPw");

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", "application/json",
                objectMapper.writeValueAsBytes(new NoticeUpdateRequest("수정된 제목", "수정된 내용"))
        );

        mockMvc.perform(multipart("/api/notices/1")
                        .file(dataPart)
                        .with(r -> { r.setMethod("PATCH"); return r; })
                        .header("Authorization", "Bearer " + token)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
    }

    @Test
    @DisplayName("공지사항 수정 성공 - 새 파일 포함")
    void updateNotice_withFile_success() throws Exception {
        String token = loginAndGetAccessToken("adminmaster@auni.ac.kr", "adminPw");

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", "application/json",
                objectMapper.writeValueAsBytes(new NoticeUpdateRequest("변경된 제목", "변경된 내용"))
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "file", "updated.pdf", "application/pdf", "updated file".getBytes()
        );

        mockMvc.perform(multipart("/api/notices/1")
                        .file(dataPart)
                        .file(filePart)
                        .with(r -> { r.setMethod("PATCH"); return r; })
                        .header("Authorization", "Bearer " + token)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("변경된 제목"));
    }

    @Test
    @DisplayName("공지사항 수정 실패 - 존재하지 않는 ID")
    void updateNotice_notFound() throws Exception {
        String token = loginAndGetAccessToken("adminmaster@auni.ac.kr", "adminPw");

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", "application/json",
                objectMapper.writeValueAsBytes(new NoticeUpdateRequest("수정", "내용"))
        );

        mockMvc.perform(multipart("/api/notices/99999")
                        .file(dataPart)
                        .with(r -> { r.setMethod("PATCH"); return r; })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("공지사항 삭제 성공")
    void deleteNotice_success() throws Exception {
        String token = loginAndGetAccessToken("adminmaster@auni.ac.kr", "adminPw");

        mockMvc.perform(delete("/api/notices/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("공지사항 삭제 성공"));
    }

    @Test
    @DisplayName("학생 권한으로 공지사항 삭제 실패")
    void deleteNotice_byStudent_thenForbidden() throws Exception {
        String token = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");

        mockMvc.perform(delete("/api/notices/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
