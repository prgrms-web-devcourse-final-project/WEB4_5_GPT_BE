package com.WEB4_5_GPT_BE.unihub.domain.member.controller;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.MemberLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.PasswordResetConfirmationRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.ProfessorSignupRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.StudentSignupRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.*;
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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MemberControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

    private String accessToken;


  @Test
  @DisplayName("학생 회원가입 - 성공")
  void signUpStudent_success() throws Exception {
      StudentSignupRequest request =
              new StudentSignupRequest(
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
      ProfessorSignupRequest request =
              new ProfessorSignupRequest(
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
    @Test
    @DisplayName("내 정보 조회 - 학생")
    void getMyInfo_student_success() throws Exception {
        MemberLoginRequest request = new MemberLoginRequest("teststudent@auni.ac.kr", "password");

        String response = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(response).path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/members/me/student")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("학생 마이페이지 조회 성공"));
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() throws Exception {
        MemberLoginRequest loginRequest = new MemberLoginRequest("teststudent@auni.ac.kr", "password");
        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        UpdatePasswordRequest request = new UpdatePasswordRequest("password", "newPassword");

        mockMvc.perform(patch("/api/members/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호 변경 성공"));
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void deleteMember_success() throws Exception {
        MemberLoginRequest loginRequest = new MemberLoginRequest("teststudent@auni.ac.kr", "password");
        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        mockMvc.perform(delete("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원 탈퇴 성공"));
    }

    @Test
    @DisplayName("전공 변경 성공")
    void changeMajor_success() throws Exception {
        MemberLoginRequest loginRequest = new MemberLoginRequest("teststudent@auni.ac.kr", "password");
        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        UpdateMajorRequest updateMajorRequest = new UpdateMajorRequest(2L);

        mockMvc.perform(patch("/api/members/me/major")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateMajorRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("전공 변경 성공"));
    }

    @Test
    @DisplayName("내 강의목록 조회 성공")
    void getLectureList_success() throws Exception {
        MemberLoginRequest loginRequest = new MemberLoginRequest("professor@auni.ac.kr", "password");
        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/members/me/courses")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("교수 강의 목록 조회 성공"));
    }

    @Test
    @DisplayName("이름 변경 성공")
    void updateName_success() throws Exception {
        MemberLoginRequest loginRequest = new MemberLoginRequest("teststudent@auni.ac.kr", "password");
        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        UpdateNameRequest request = new UpdateNameRequest("새이름");

        mockMvc.perform(patch("/api/members/me/name")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이름 변경 성공"));
    }

    @Test
    @DisplayName("이메일 변경 성공")
    void updateEmail_success() throws Exception {
        MemberLoginRequest loginRequest = new MemberLoginRequest("teststudent@auni.ac.kr", "password");
        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        UpdateEmailRequest request = new UpdateEmailRequest("newemail@auni.ac.kr");

        mockMvc.perform(patch("/api/members/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 변경 성공"));
    }

    @Test
    @DisplayName("비밀번호 검증 성공")
    void verifyPassword_success() throws Exception {
        MemberLoginRequest loginRequest = new MemberLoginRequest("teststudent@auni.ac.kr", "password");
        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        VerifyPasswordRequest request = new VerifyPasswordRequest("password");

        mockMvc.perform(post("/api/members/me/verify-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호 검증 성공"));
    }

}
