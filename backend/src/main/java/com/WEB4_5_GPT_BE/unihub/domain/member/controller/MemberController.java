package com.WEB4_5_GPT_BE.unihub.domain.member.controller;

import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.*;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.*;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.AdminLoginResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.MemberLoginResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.MyPageProfessorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.MyPageStudentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.ProfessorCourseResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.UpdateMajorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.enums.VerificationPurpose;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.AuthService;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.MemberService;
import com.WEB4_5_GPT_BE.unihub.global.response.Empty;
import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Member", description = "회원 관련 API (회원가입, 로그인, 마이페이지, 탈퇴 등)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

  private final MemberService memberService;
  private final AuthService authService;

  @Operation(summary = "학생 회원가입", description = "학생 계정으로 회원가입을 진행합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "회원가입 성공"),
          @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 누락, 이메일 미인증, 학교 이메일 도메인 불일치)"),
          @ApiResponse(responseCode = "404", description = "존재하지 않는 대학 또는 전공"),
          @ApiResponse(responseCode = "409", description = "이메일 또는 학번이 이미 등록되어 있습니다.")
  })
  @PostMapping("/signup/student")
  public RsData<Empty> signUpStudent(@RequestBody @Valid StudentSignUpRequest request) {
    memberService.signUpStudent(request);
    return new RsData<>("200", "학생 가입이 완료되었습니다.");
  }

  @Operation(summary = "교수 회원가입", description = "교수 계정으로 회원가입을 진행하며, 관리자의 승인을 기다려야 합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "회원가입 성공"),
          @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 누락, 이메일 미인증, 학교 이메일 도메인 불일치)"),
          @ApiResponse(responseCode = "404", description = "존재하지 않는 대학 또는 전공"),
          @ApiResponse(responseCode = "409", description = "이메일 또는 학번이 이미 등록되어 있습니다.")
  })
  @PostMapping("/signup/professor")
  public RsData<Empty> signUpProfessor(@RequestBody @Valid ProfessorSignUpRequest request) {
    memberService.signUpProfessor(request);
    return new RsData<>("201", "교직원 가입 신청이 완료되었습니다. 관리자의 승인을 기다려 주세요.");
  }

  @Operation(summary = "이메일 인증 코드 전송", description = "입력한 이메일 주소로 인증 코드를 전송합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "인증 코드 전송 성공"),
          @ApiResponse(responseCode = "400", description = "잘못된 이메일 형식 또는 이미 인증된 이메일"),
          @ApiResponse(responseCode = "500", description = "이메일 발송 실패")
  })
  @PostMapping("/email/{purpose}/code")
  public RsData<Empty> sendEmail(
          @Parameter(description = "이메일 인증 목적 (SIGNUP, PASSWORD_RESET, EMAIL_CHANGE 등)", schema = @Schema(implementation = VerificationPurpose.class))
          @PathVariable VerificationPurpose purpose,
          @RequestBody @Valid EmailCodeRequest request
  ) {
      memberService.sendVerificationCode(request.email(), purpose);
      return new RsData<>("200", "이메일 인증번호가 전송되었습니다. 확인 후 인증번호를 입력해 주세요.");
  }

    @Operation(summary = "이메일 인증 확인", description = "사용자가 입력한 인증번호를 검증하고 이메일 인증을 완료합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 인증번호이거나 인증되지 않은 이메일입니다.")
    })
    @PostMapping("/email/{purpose}/verify")
    public RsData<Empty> verifyEmail(
            @Parameter(description = "이메일 인증 목적 (SIGNUP, PASSWORD_RESET, EMAIL_CHANGE 등)", schema = @Schema(implementation = VerificationPurpose.class))
            @PathVariable VerificationPurpose purpose,
            @RequestBody @Valid EmailCodeVerificationRequest request
    ) {
        memberService.verifyEmailCode(request.email(), request.emailCode(), purpose);
        return new RsData<>("200", "이메일 인증이 완료되었습니다.");
    }

  @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 검증 후, 새 비밀번호로 변경합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
          @ApiResponse(responseCode = "400", description = "잘못된 이메일 또는 비밀번호 형식"),
          @ApiResponse(responseCode = "404", description = "등록되지 않은 이메일 주소")
  })
  @PostMapping("/password-reset/confirm")
  public RsData<Empty> resetPassword(@RequestBody @Valid PasswordResetConfirmationRequest request) {
    memberService.resetPassword(request);
    return new RsData<>("200", "비밀번호가 성공적으로 변경되었습니다.");
  }

  @Operation(summary = "일반 사용자 로그인", description = "일반 회원 로그인을 수행하고, accessToken과 refreshToken을 발급합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "로그인 성공"),
          @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치, 승인되지 않은 교직원, 비밀번호 5회 이상 오류"),
          @ApiResponse(responseCode = "429", description = "로그인 시도 제한 (비밀번호 5회 이상 오류)")
  })
  @PostMapping("/login")
  public RsData<MemberLoginResponse> login(@RequestBody @Valid MemberLoginRequest request) {
    MemberLoginResponse response = authService.login(request);
    return new RsData<>("200", "로그인에 성공했습니다.", response);
  }

  @Operation(summary = "관리자 로그인", description = "관리자 로그인을 수행하고, 관리자용 토큰을 발급합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "관리자 로그인 성공"),
          @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치, 비밀번호 5회 이상 오류"),
          @ApiResponse(responseCode = "403", description = "관리자 권한이 없습니다.")
  })
  @PostMapping("/login/admin")
  public RsData<AdminLoginResponse> adminLogin(@RequestBody @Valid AdminLoginRequest request) {
    AdminLoginResponse response = authService.adminLogin(request);
    return new RsData<>("200", "관리자 로그인 성공.", response);
  }

  @Operation(summary = "로그아웃", description = "현재 로그인된 사용자를 로그아웃 처리합니다. 서버 저장소에서 refreshToken을 제거합니다.")
  @SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
          @ApiResponse(responseCode = "401", description = "인증 토큰 누락 또는 유효하지 않은 형식")
  })
  @PostMapping("/logout")
  public RsData<Empty> logout(HttpServletRequest request, HttpServletResponse response) {
    authService.logout(request, response);
    return new RsData<>("200", "로그아웃에 성공했습니다.");
  }

  @Operation(
          summary = "AccessToken 재발급",
          description = "RefreshToken을 사용하여 새로운 AccessToken을 발급받습니다. RefreshToken은 쿠키에서 자동 추출됩니다."
  )
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
          @ApiResponse(responseCode = "401", description = "RefreshToken이 유효하지 않거나 존재하지 않습니다.")
  })
  @PostMapping("/refresh")
  public RsData<MemberLoginResponse> refreshToken(
      HttpServletRequest request, HttpServletResponse response) {
    MemberLoginResponse tokenResponse = authService.refreshAccessToken(request, response);
    return new RsData<>("200", "새로운 토큰이 발급되었습니다.", tokenResponse);
  }

    @Operation(
            summary = "학생 마이페이지 조회",
            description = "현재 로그인한 학생의 프로필 및 전공/학년/학기 정보를 반환합니다."

    )
    @SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "학생 마이페이지 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않았거나 권한이 없는 사용자")
    })
    @GetMapping("/me/student")
    public RsData<MyPageStudentResponse> getStudentMyPage(@AuthenticationPrincipal SecurityUser user) {
        return new RsData<>("200", "학생 마이페이지 조회 성공", memberService.getStudentMyPage(user.getId()));
    }

    @Operation(
            summary = "교수 마이페이지 조회",
            description = "현재 로그인한 교수의 프로필 및 소속 대학/전공 정보를 반환합니다."

    )
    @SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "교수 마이페이지 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않았거나 권한이 없는 사용자")
    })
    @GetMapping("/me/professor")
    public RsData<MyPageProfessorResponse> getProfessorMyPage(@AuthenticationPrincipal SecurityUser user) {
        return new RsData<>("200", "교수 마이페이지 조회 성공", memberService.getProfessorMyPage(user.getId()));
    }

    @Operation(
            summary = "교수 강의 목록 조회",
            description = "현재 로그인한 교수의 담당 강의 리스트를 반환합니다."

    )
    @SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "강의 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않았거나 권한이 없는 사용자")
    })
    @GetMapping("/me/courses")
    public RsData<List<ProfessorCourseResponse>> getProfessorCourses(@AuthenticationPrincipal SecurityUser user) {
        return new RsData<>("200", "교수 강의 목록 조회 성공", memberService.getProfessorCourses(user.getId()));
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "현재 비밀번호를 검증한 후 새 비밀번호로 변경합니다."

    )
    @SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치 또는 잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PatchMapping("/me/password")
    public RsData<Void> updatePassword(@AuthenticationPrincipal SecurityUser user,
                                       @RequestBody @Valid UpdatePasswordRequest request) {
        memberService.updatePassword(user.getId(), request);
        return new RsData<>("200", "비밀번호 변경 성공");
    }

    @Operation(
            summary = "이메일 변경",
            description = "새 이메일로 변경합니다."

    )
    @SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 변경 성공"),
            @ApiResponse(responseCode = "400", description = "동일 이메일 입력 또는 잘못된 요청"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PatchMapping("/me/email")
    public RsData<Void> updateEmail(@AuthenticationPrincipal SecurityUser user,
                                    @RequestBody @Valid UpdateEmailRequest request) {
        memberService.updateEmail(user.getId(), request);
        return new RsData<>("200", "이메일 변경 성공");
    }

    @Operation(
            summary = "전공 변경",
            description = "학생의 전공을 변경합니다."

    )
    @SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전공 변경 성공"),
            @ApiResponse(responseCode = "400", description = "동일 전공 입력 또는 잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "학생만 접근 가능한 기능"),
            @ApiResponse(responseCode = "404", description = "해당 전공을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PatchMapping("/me/major")
    public RsData<UpdateMajorResponse> updateMajor(@AuthenticationPrincipal SecurityUser user,
                                                   @RequestBody @Valid UpdateMajorRequest request) {
        return new RsData<>("200", "전공 변경 성공", memberService.updateMajor(user.getId(), request));
    }

    @Operation(
            summary = "현재 비밀번호 검증",
            description = "입력된 비밀번호가 현재 비밀번호와 일치하는지 검증합니다.(페이지 접근용)"

    )
    @SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 검증 성공"),
            @ApiResponse(responseCode = "403", description = "비밀번호 불일치"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/me/verify-password")
    public RsData<Void> verifyPassword(@AuthenticationPrincipal SecurityUser user,
                                       @RequestBody @Valid VerifyPasswordRequest request) {
        memberService.verifyPassword(user.getId(), request);
        return new RsData<>("200", "비밀번호 검증 성공");
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인된 회원을 탈퇴 처리합니다."

    )
    @SecurityRequirement(name = "accessToken을 사용한 bearerAuth 로그인 인증")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @DeleteMapping("/me")
    public RsData<Void> deleteMember(@AuthenticationPrincipal SecurityUser user) {
        memberService.deleteMember(user.getId());
        return new RsData<>("200", "회원 탈퇴 성공");
    }
}
