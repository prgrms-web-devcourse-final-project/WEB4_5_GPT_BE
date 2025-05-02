package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.AdminLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.MemberLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.AdminLoginResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.MemberLoginResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.global.Rq;
import com.WEB4_5_GPT_BE.unihub.global.config.RedisTestContainerConfig;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RedisTestContainerConfig
class AuthServiceImplTest {

  @InjectMocks private AuthServiceImpl authService;

  @Mock private MemberRepository memberRepository;

  @Mock private Rq rq;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private AuthTokenService authTokenService;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private StringRedisTemplate redisTemplate;

  @Test
  @DisplayName("로그인 성공 시 accessToken과 refreshToken을 발급하고 반환한다")
  void givenValidLoginRequest_whenLogin_thenReturnTokens() {
    // given
    String email = "test@example.com";
    String password = "password";
    MemberLoginRequest request = new MemberLoginRequest(email, password);

    Member member = Member.builder().email(email).password("encodedPassword").build();

    when(redisTemplate.opsForValue().get("login:fail:" + email)).thenReturn("0");
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
    when(passwordEncoder.matches(password, member.getPassword())).thenReturn(true);
    when(authTokenService.genAccessToken(member)).thenReturn("accessToken");
    when(authTokenService.genRefreshToken(member.getId())).thenReturn("refreshToken");

    // when
    MemberLoginResponse response = authService.login(request);

    // then
    verify(redisTemplate).delete("login:fail:" + email);
    verify(redisTemplate.opsForValue())
        .set(eq("refresh:" + member.getId()), eq("refreshToken"), eq(java.time.Duration.ofDays(7)));
    verify(authTokenService).genAccessToken(member);
    verify(authTokenService).genRefreshToken(member.getId());

    assertThat(response.accessToken()).isEqualTo("accessToken");
    assertThat(response.refreshToken()).isEqualTo("refreshToken");
  }

  @Test
  @DisplayName("존재하지 않는 이메일로 로그인 시 실패한다")
  void givenNonExistentEmail_whenLogin_thenThrowUnihubException() {
    // given
    String email = "nonexistent@example.com";
    MemberLoginRequest request = new MemberLoginRequest(email, "password");

    when(redisTemplate.opsForValue().get("login:fail:" + email)).thenReturn("0");
    when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(UnihubException.class)
        .hasMessageContaining("이메일 또는 비밀번호가 잘못되었습니다.");
  }

  @Test
  @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
  void givenInvalidPassword_whenLogin_thenThrowUnihubException() {
    // given
    String email = "test@example.com";
    String password = "wrongPassword";
    MemberLoginRequest request = new MemberLoginRequest(email, password);

    Member member = Member.builder().email(email).password("encodedPassword").build();

    when(redisTemplate.opsForValue().get("login:fail:" + email)).thenReturn("0");
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
    when(passwordEncoder.matches(password, member.getPassword())).thenReturn(false);

    // when / then
    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(UnihubException.class)
        .hasMessageContaining("이메일 또는 비밀번호가 잘못되었습니다.");
  }

  @Test
  @DisplayName("로그인 실패 횟수가 5회를 초과하면 로그인 제한 예외를 발생시킨다")
  void givenExceededFailCount_whenLogin_thenThrowLoginLockException() {
    // given
    String email = "locked@example.com";
    MemberLoginRequest request = new MemberLoginRequest(email, "password");

    when(redisTemplate.opsForValue().get("login:fail:" + email)).thenReturn("5");

    // when / then
    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(UnihubException.class)
        .hasMessageContaining("비밀번호 오류 5회 이상. 5분간 로그인이 제한됩니다.");
  }

  @Test
  @DisplayName("관리자 로그인 성공 시 accessToken과 refreshToken을 발급하고 반환한다")
  void givenValidAdminLoginRequest_whenAdminLogin_thenReturnTokens() {
    // given
    String email = "admin@example.com";
    String password = "adminPassword";
    AdminLoginRequest request = new AdminLoginRequest(email, password);

    Member adminMember =
        Member.builder()
            .email(email)
            .password("encodedPassword")
            .role(com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role.ADMIN)
            .build();

    when(redisTemplate.opsForValue().get("login:fail:" + email)).thenReturn("0");
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(adminMember));
    when(passwordEncoder.matches(password, adminMember.getPassword())).thenReturn(true);
    when(authTokenService.genAccessToken(adminMember)).thenReturn("adminAccessToken");
    when(authTokenService.genRefreshToken(adminMember.getId())).thenReturn("adminRefreshToken");

    // when
    AdminLoginResponse response = authService.adminLogin(request);

    // then
    verify(redisTemplate).delete("login:fail:" + email);
    verify(redisTemplate.opsForValue())
        .set(
            eq("refresh:" + adminMember.getId()),
            eq("adminRefreshToken"),
            eq(java.time.Duration.ofDays(7)));
    verify(authTokenService).genAccessToken(adminMember);
    verify(authTokenService).genRefreshToken(adminMember.getId());

    assertThat(response.accessToken()).isEqualTo("adminAccessToken");
    assertThat(response.refreshToken()).isEqualTo("adminRefreshToken");
  }

  @Test
  @DisplayName("일반 사용자가 관리자 로그인을 시도하면 권한 없음 예외가 발생한다")
  void givenNonAdminUser_whenAdminLogin_thenThrowPermissionException() {
    // given
    String email = "student@example.com";
    String password = "studentPassword";
    AdminLoginRequest request = new AdminLoginRequest(email, password);

    Member studentMember =
        Member.builder()
            .email(email)
            .password("encodedPassword")
            .role(com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role.STUDENT)
            .build();

    when(redisTemplate.opsForValue().get("login:fail:" + email)).thenReturn("0");
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(studentMember));
    when(passwordEncoder.matches(password, studentMember.getPassword())).thenReturn(true);

    // when / then
    assertThatThrownBy(() -> authService.adminLogin(request))
        .isInstanceOf(UnihubException.class)
        .hasMessageContaining("관리자 권한이 없습니다.");
  }

  @Test
  @DisplayName("AccessToken을 기반으로 로그아웃 시 RefreshToken 삭제 및 쿠키 무효화가 수행된다")
  void givenValidAccessToken_whenLogout_thenRefreshTokenDeletedAndCookieExpired() {
    // given
    String accessToken = "Bearer validAccessToken";
    Long memberId = 1L;
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader("Authorization", accessToken);

    // mock 설정
    given(rq.getAccessToken()).willReturn("validAccessToken");
    given(authTokenService.getMemberIdFromToken("validAccessToken")).willReturn(memberId);
    doNothing().when(rq).removeCookie("refreshToken");

    // when
    authService.logout(request, response);

    // then
    verify(redisTemplate).delete("refresh:" + memberId);
    verify(rq).removeCookie("refreshToken");
  }

  @Test
  @DisplayName("Authorization 헤더가 없으면 인증 예외를 발생시킨다")
  void givenNoAccessToken_whenLogout_thenThrowAuthenticationException() {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest(); // Authorization 없음
    MockHttpServletResponse response = new MockHttpServletResponse();

    // when / then
    assertThatThrownBy(() -> authService.logout(request, response))
        .isInstanceOf(UnihubException.class)
        .hasMessageContaining("인증이 필요합니다.");
  }

  @Test
  @DisplayName("승인되지 않은 교직원 계정은 로그인할 수 없다")
  void givenUnapprovedProfessor_whenLogin_thenThrowApprovalException() {
    // given
    String email = "pending@auni.ac.kr";
    String password = "password";
    MemberLoginRequest request = new MemberLoginRequest(email, password);

    Member pendingProfessor = Member.builder()
            .email(email)
            .password("encodedPassword")
            .role(Role.PROFESSOR)
            .professorProfile(
                    com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile.builder()
                            .approvalStatus(com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus.PENDING)
                            .build()
            )
            .build();

    when(redisTemplate.opsForValue().get("login:fail:" + email)).thenReturn("0");
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(pendingProfessor));
    when(passwordEncoder.matches(password, pendingProfessor.getPassword())).thenReturn(true);

    // when / then
    assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UnihubException.class)
            .hasMessageContaining("아직 승인이 완료되지 않은 교직원 계정입니다.");
  }

  @Test
  @DisplayName("AccessToken이 유효하지 않으면 유효하지 않은 형식 예외를 발생시킨다")
  void givenInvalidAccessToken_whenLogout_thenThrowInvalidTokenException() {
    // given
    String accessToken = "Bearer invalidToken";
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader("Authorization", accessToken);

    given(rq.getAccessToken()).willReturn("invalidToken");

    given(authTokenService.getMemberIdFromToken("invalidToken")).willReturn(null);

    // when / then
    assertThatThrownBy(() -> authService.logout(request, response))
        .isInstanceOf(UnihubException.class)
        .hasMessageContaining("유효하지 않은 형식.");
  }

  @Test
  @DisplayName("유효한 RefreshToken으로 accessToken을 재발급한다")
  void givenValidRefreshToken_whenRefreshAccessToken_thenReturnAccessToken() {
    // given
    Long memberId = 1L;
    String refreshToken = "validRefreshToken";
    String newAccessToken = "newAccessToken";

    Member member =
        Member.builder().id(memberId).email("test@example.com").role(Role.STUDENT).build();

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("refreshToken", refreshToken));
    MockHttpServletResponse response = new MockHttpServletResponse();

    given(authTokenService.validateRefreshToken(refreshToken)).willReturn(true);
    given(authTokenService.getMemberIdFromToken(refreshToken)).willReturn(memberId);
    given(redisTemplate.opsForValue().get("refresh:" + memberId)).willReturn(refreshToken);
    given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
    given(authTokenService.genAccessToken(member)).willReturn(newAccessToken);
    given(rq.getRefreshToken()).willReturn(refreshToken);

    // when
    MemberLoginResponse loginResponse = authService.refreshAccessToken(request, response);

    // then
    assertThat(loginResponse.accessToken()).isEqualTo(newAccessToken);
    assertThat(loginResponse.refreshToken()).isNull(); // 재발급 안 하는 경우
  }

  @Test
  @DisplayName("refreshToken 쿠키가 없으면 예외가 발생한다")
  void givenNoRefreshTokenCookie_whenRefresh_thenThrowException() {
    MockHttpServletRequest request = new MockHttpServletRequest(); // 쿠키 없음
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThatThrownBy(() -> authService.refreshAccessToken(request, response))
        .isInstanceOf(UnihubException.class)
        .hasMessageContaining("Refresh Token이 존재하지 않습니다.");
  }

  @Test
  @DisplayName("refreshToken이 유효하지 않으면 예외를 발생시킨다")
  void givenInvalidRefreshToken_whenRefresh_thenThrowException() {
    // given
    String refreshToken = "invalid";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("refreshToken", refreshToken));
    MockHttpServletResponse response = new MockHttpServletResponse();

    given(rq.getRefreshToken()).willReturn(refreshToken);

    given(authTokenService.validateRefreshToken(refreshToken)).willReturn(false);

    // when / then
    assertThatThrownBy(() -> authService.refreshAccessToken(request, response))
        .isInstanceOf(UnihubException.class)
        .hasMessageContaining("유효하지 않은 refreshToken입니다.");
  }

  @Test
  @DisplayName("Redis에 저장된 refreshToken과 다르면 예외를 발생시킨다")
  void givenMismatchRedisToken_whenRefresh_thenThrowException() {
    String refreshToken = "tokenX";
    Long memberId = 1L;

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("refreshToken", refreshToken));
    MockHttpServletResponse response = new MockHttpServletResponse();

    given(rq.getRefreshToken()).willReturn(refreshToken);
    given(authTokenService.validateRefreshToken(refreshToken)).willReturn(true);
    given(authTokenService.getMemberIdFromToken(refreshToken)).willReturn(memberId);
    given(redisTemplate.opsForValue().get("refresh:" + memberId)).willReturn("differentToken");

    assertThatThrownBy(() -> authService.refreshAccessToken(request, response))
        .isInstanceOf(UnihubException.class)
        .hasMessageContaining("Refresh Token 정보가 일치하지 않습니다.");
  }
}
