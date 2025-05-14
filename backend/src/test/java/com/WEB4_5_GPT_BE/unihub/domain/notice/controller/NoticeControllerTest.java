package com.WEB4_5_GPT_BE.unihub.domain.notice.controller;

import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.MemberLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request.NoticeCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.notice.dto.request.NoticeUpdateRequest;
import com.WEB4_5_GPT_BE.unihub.global.config.RedisTestContainerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
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

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        var loginRequest = new com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.MemberLoginRequest(email, password);
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
        // 1. 로그인
        MemberLoginRequest loginRequest = new MemberLoginRequest("adminmaster@auni.ac.kr", "adminPw");

        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        // 2. 공지사항 목록 조회
        mockMvc.perform(get("/api/notices")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(greaterThan(0)));
    }

    @Test
    @DisplayName("공지사항 상세 조회 성공")
    void getNotice_success() throws Exception {
        // 1. 로그인
        MemberLoginRequest loginRequest = new MemberLoginRequest("adminmaster@auni.ac.kr", "adminPw");

        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        // 2. 공지사항 상세 조회
        mockMvc.perform(get("/api/notices/1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("필독 공지"));
    }

    @Test
    @DisplayName("공지사항 작성 성공")
    void createNotice_success() throws Exception {
        String token = loginAndGetAccessToken("adminmaster@auni.ac.kr", "adminPw");
        NoticeCreateRequest request = new NoticeCreateRequest("새 공지", "공지 내용", null);

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("새 공지"));
    }

    @Test
    @DisplayName("학생 권한으로 공지사항 작성 실패")
    void createNotice_byStudent_thenForbidden() throws Exception {
        String token = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");
        NoticeCreateRequest request = new NoticeCreateRequest("학생 공지", "학생이 작성", null);

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("공지사항 수정 성공")
    void updateNotice_success() throws Exception {
        String token = loginAndGetAccessToken("adminmaster@auni.ac.kr", "adminPw");
        NoticeUpdateRequest request = new NoticeUpdateRequest("수정된 제목", "수정된 내용", null);

        mockMvc.perform(patch("/api/notices/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
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
}
