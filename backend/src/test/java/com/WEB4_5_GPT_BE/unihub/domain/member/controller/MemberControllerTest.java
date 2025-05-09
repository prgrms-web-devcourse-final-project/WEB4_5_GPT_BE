package com.WEB4_5_GPT_BE.unihub.domain.member.controller;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.*;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.*;
import com.WEB4_5_GPT_BE.unihub.domain.member.enums.VerificationPurpose;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.EmailService;
import com.WEB4_5_GPT_BE.unihub.global.config.RedisTestContainerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RedisTestContainerConfig
public class MemberControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmailService emailService;

    @TestConfiguration
    static class EmailServiceMockConfig {
        @Bean
        public EmailService emailService() {
            EmailService mock = mock(EmailService.class);
            when(mock.isAlreadyVerified(anyString(), any(VerificationPurpose.class))).thenReturn(true);


            doNothing().when(mock).sendVerificationCode(anyString(), any(VerificationPurpose.class));


            doNothing().when(mock).markEmailAsVerified(anyString(), any(VerificationPurpose.class));
            return mock;
        }
    }

    @Test
    @DisplayName("학생 회원가입 - 성공")
    void signUpStudent_success() throws Exception {
        emailService.markEmailAsVerified("haneulkim@auni.ac.kr",VerificationPurpose.SIGNUP);

        StudentSignUpRequest request = new StudentSignUpRequest(
                "haneulkim@auni.ac.kr", "password", "김하늘", "20250001", 1L, 1L, 1, 1, Role.STUDENT);

        mockMvc.perform(post("/api/members/signup/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("학생 가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("교직원 회원가입 - 성공")
    void signUpProfessor_success() throws Exception {
        emailService.markEmailAsVerified("kim@auni.ac.kr",VerificationPurpose.SIGNUP);

        ProfessorSignUpRequest request = new ProfessorSignUpRequest(
                "kim@auni.ac.kr", "password", "김교수", "20250001", 1L, 1L, Role.PROFESSOR);

        mockMvc.perform(post("/api/members/signup/professor")
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

        mockMvc.perform(post("/api/members/login")
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

        String responseBody = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(responseBody).path("data").path("accessToken").asText();

        mockMvc.perform(post("/api/members/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃에 성공했습니다."));
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void refreshToken_success() throws Exception {
        MemberLoginRequest loginRequest = new MemberLoginRequest("teststudent@auni.ac.kr", "password");

        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(loginResponse).path("data").path("refreshToken").asText();

        mockMvc.perform(post("/api/members/refresh").cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("새로운 토큰이 발급되었습니다."))
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @DisplayName("비밀번호 재설정 성공")
    void resetPassword_success() throws Exception {
        String email = "teststudent2@auni.ac.kr";
        String code = "123456"; // 인증 코드 예시

        // 1. 로그인하여 accessToken 확보
        MemberLoginRequest loginRequest = new MemberLoginRequest("teststudent2@auni.ac.kr", "password");

        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        // 2. 이메일 인증
        EmailCodeVerificationRequest verificationRequest = new EmailCodeVerificationRequest(email, code);
        mockMvc.perform(post("/api/members/email/PASSWORD_RESET/verify")
                        .header("Authorization", "Bearer " + accessToken) // 로그인된 사용자의 토큰 추가
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."));

        // 3. 비밀번호 재설정
        PasswordResetConfirmationRequest request = new PasswordResetConfirmationRequest(email, "newpassword123");

        mockMvc.perform(post("/api/members/password-reset/confirm")
                        .header("Authorization", "Bearer " + accessToken) // 로그인된 사용자의 토큰 추가
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 변경되었습니다."));
    }

    @Test
    @DisplayName("승인되지 않은 교직원은 로그인에 실패한다")
    void login_unapprovedProfessor_thenFail() throws Exception {
        MemberLoginRequest request = new MemberLoginRequest("pending@auni.ac.kr", "password");

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("아직 승인이 완료되지 않은 교직원 계정입니다."));
    }

    @Test
    @DisplayName("이메일 인증 없이 학생 회원가입 시 실패한다")
    void signUpStudent_withoutEmailVerification_thenFail() throws Exception {
        when(emailService.isAlreadyVerified("unverified@auni.ac.kr", VerificationPurpose.SIGNUP)).thenReturn(false);

        StudentSignUpRequest request = new StudentSignUpRequest(
                "unverified@auni.ac.kr", "password", "학생", "20251234", 1L, 1L, 1, 1, Role.STUDENT);

        mockMvc.perform(post("/api/members/signup/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이메일 인증을 완료해주세요."));
    }

    @Test
    @DisplayName("학생 회원가입 실패 - 이메일 도메인이 학교와 일치하지 않음")
    void signUpStudent_invalidEmailDomain_thenFail() throws Exception {
        String email = "wrong@notmatched.com";
        when(emailService.isAlreadyVerified(email,VerificationPurpose.SIGNUP)).thenReturn(true);

        StudentSignUpRequest request = new StudentSignUpRequest(
                email, "password", "학생", "20251234", 1L, 1L, 1, 1, Role.STUDENT);

        mockMvc.perform(post("/api/members/signup/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("선택한 학교의 이메일 형식(@auni.ac.kr)과 일치하지 않습니다."));
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
        // 1) 로그인하여 access token 확보
        MemberLoginRequest loginRequest =
                new MemberLoginRequest("professor@auni.ac.kr", "password");

        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse)
                .path("data").path("accessToken").asText();

        // 2) 강의 목록 조회 + JSON 검증
        mockMvc.perform(get("/api/members/me/courses")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("교수 강의 목록 조회 성공"))

                //data 배열 및 첫 원소 필드 검증
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)))

                .andExpect(jsonPath("$.data[0].title").value("컴파일러"))
                .andExpect(jsonPath("$.data[0].location").value("공학관A"))
                .andExpect(jsonPath("$.data[0].capacity").value(200))
                .andExpect(jsonPath("$.data[0].schedule[0].day").value("MON"))
                .andExpect(jsonPath("$.data[0].schedule[0].startTime").value("12:00"))
                .andExpect(jsonPath("$.data[0].schedule[0].endTime").value("14:00"));
    }



    @Test
    @DisplayName("이메일 변경 성공")
    void updateEmail_success() throws Exception {
        // 1. 로그인 요청
        MemberLoginRequest loginRequest = new MemberLoginRequest("teststudent@auni.ac.kr", "password");
        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        // 2. 이메일 인증 코드 발송
        EmailCodeVerificationRequest verificationRequest = new EmailCodeVerificationRequest("newemail@auni.ac.kr", "123456");
        mockMvc.perform(post("/api/members/email/EMAIL_CHANGE/verify")  // EMAIL_CHANGE로 수정
                        .header("Authorization", "Bearer " + accessToken)  // 로그인된 사용자의 토큰 추가
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."));

        // 3. 이메일 변경 요청
        UpdateEmailRequest request = new UpdateEmailRequest("newemail@auni.ac.kr");

        // 4. 이메일 변경 API 호출
        mockMvc.perform(patch("/api/members/me/email")
                        .header("Authorization", "Bearer " + accessToken)  // Bearer token 포함
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
