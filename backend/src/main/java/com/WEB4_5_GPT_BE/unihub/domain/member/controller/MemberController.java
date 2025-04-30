package com.WEB4_5_GPT_BE.unihub.domain.member.controller;

import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.*;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.AdminLoginResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.MemberLoginResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.AuthService;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.MemberService;
import com.WEB4_5_GPT_BE.unihub.global.response.Empty;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

  private final MemberService memberService;
  private final AuthService authService;

  @PostMapping("/signup/student")
  public RsData<Empty> signUpStudent(@RequestBody @Valid StudentSignUpRequest request) {
    memberService.signUpStudent(request);
    return new RsData<>("200", "학생 가입이 완료되었습니다.");
  }

  @PostMapping("/signup/professor")
  public RsData<Empty> signUpProfessor(@RequestBody @Valid ProfessorSignUpRequest request) {
    memberService.signUpProfessor(request);
    return new RsData<>("201", "교직원 가입 신청이 완료되었습니다. 관리자의 승인을 기다려 주세요.");
  }

  @PostMapping("/email/code")
  public RsData<Empty> sendEmail(@RequestBody @Valid EmailCodeRequest request) {
    memberService.sendVerificationCode(request.email());
    return new RsData<>("200", "이메일 인증번호가 전송되었습니다. 확인 후 인증번호를 입력해 주세요.");
  }

  @PostMapping("/email/verify")
  public RsData<Empty> verifyEmail(@RequestBody @Valid EmailCodeVerificationRequest request) {
    memberService.verifyEmailCode(request);
    return new RsData<>("200", "이메일 인증이 완료되었습니다.");
  }

  @PostMapping("/password-reset/confirm")
  public RsData<Empty> resetPassword(@RequestBody @Valid PasswordResetConfirmationRequest request) {
    memberService.resetPassword(request);
    return new RsData<>("200", "비밀번호가 성공적으로 변경되었습니다.");
  }

  @PostMapping("/login")
  public RsData<MemberLoginResponse> login(@RequestBody @Valid MemberLoginRequest request) {
    MemberLoginResponse response = authService.login(request);
    return new RsData<>("200", "로그인에 성공했습니다.", response);
  }

  @PostMapping("/login/admin")
  public RsData<AdminLoginResponse> adminLogin(@RequestBody @Valid AdminLoginRequest request) {
    AdminLoginResponse response = authService.adminLogin(request);
    return new RsData<>("200", "관리자 로그인 성공.", response);
  }

  @PostMapping("/logout")
  public RsData<Empty> logout(HttpServletRequest request, HttpServletResponse response) {
    authService.logout(request, response);
    return new RsData<>("200", "로그아웃에 성공했습니다.");
  }

  @PostMapping("/refresh")
  public RsData<MemberLoginResponse> refreshToken(
      HttpServletRequest request, HttpServletResponse response) {
    MemberLoginResponse tokenResponse = authService.refreshAccessToken(request, response);
    return new RsData<>("200", "새로운 토큰이 발급되었습니다.", tokenResponse);
  }
}
