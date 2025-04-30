package com.WEB4_5_GPT_BE.unihub.domain.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class MemberControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("학생 회원가입 - 성공")
  void signUpStudent_success() throws Exception {
    StudentSignUpRequest request =
        new StudentSignUpRequest(
            "haneulkim@auni.ac.kr", "password", "김하늘", "20250001", 1L, 1L, 1, 1, Role.STUDENT);

    mockMvc
        .perform(
            post("/api/members/signup/student")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("200"))
        .andExpect(jsonPath("$.message").value("학생 가입이 완료되었습니다."));
  }

  @Test
  @DisplayName("교직원 회원가입 - 성공")
  void signUpProfessor_success() throws Exception {
    ProfessorSignUpRequest request =
        new ProfessorSignUpRequest(
            "kim@auni.ac.kr", "password", "김교수", "20250001", 1L, 1L, Role.PROFESSOR);

    mockMvc
        .perform(
            post("/api/members/signup/professor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("201"))
        .andExpect(jsonPath("$.message").value("교직원 가입 신청이 완료되었습니다. 관리자의 승인을 기다려 주세요."));
  }

  @Test
  @DisplayName("로그인 성공")
  void login_success() throws Exception {
    MemberLoginRequest request = new MemberLoginRequest("teststudent@auni.ac.kr", "password");

    mockMvc
        .perform(
            post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
        .andExpect(jsonPath("$.data.accessToken").exists())
        .andExpect(jsonPath("$.data.refreshToken").exists());
  }

  @Test
  @DisplayName("로그아웃 성공")
  void logout_success() throws Exception {

    MemberLoginRequest loginRequest = new MemberLoginRequest("teststudent@auni.ac.kr", "password");

    String responseBody =
        mockMvc
            .perform(
                post("/api/members/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String accessToken =
        objectMapper.readTree(responseBody).path("data").path("accessToken").asText();

    mockMvc
        .perform(post("/api/members/logout").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("로그아웃에 성공했습니다."));
  }

  @Test
  @DisplayName("토큰 재발급 성공")
  void refreshToken_success() throws Exception {
    MemberLoginRequest loginRequest = new MemberLoginRequest("teststudent@auni.ac.kr", "password");

    String loginResponse =
        mockMvc
            .perform(
                post("/api/members/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String refreshToken =
        objectMapper.readTree(loginResponse).path("data").path("refreshToken").asText();

    mockMvc
        .perform(post("/api/members/refresh").cookie(new Cookie("refreshToken", refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("새로운 토큰이 발급되었습니다."))
        .andExpect(jsonPath("$.data.accessToken").exists());
  }

  @Test
  @DisplayName("비밀번호 재설정 성공")
  void resetPassword_success() throws Exception {
    PasswordResetConfirmationRequest request =
        new PasswordResetConfirmationRequest("teststudent2@auni.ac.kr", "123456");

    mockMvc
        .perform(
            post("/api/members/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 변경되었습니다."));
  }
}
