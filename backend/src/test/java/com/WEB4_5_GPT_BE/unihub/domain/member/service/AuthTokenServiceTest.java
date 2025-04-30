package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.TokenMemberPayload;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthTokenServiceTest {

  private AuthTokenService authTokenService;

  @BeforeEach
  void setUp() {
    authTokenService =
        new AuthTokenService(
            "your_test_secret_key_for_testing_purposes_1234567890!!", // 테스트용 시크릿 키
            1800, // 액세스 토큰 유효시간 30분
            604800 // 리프레시 토큰 유효시간 7일
            );
  }

  @Test
  @DisplayName("Member 정보로 AccessToken을 생성한다")
  void givenMember_whenGenAccessToken_thenReturnValidToken() {
    Member member = Member.builder().id(1L).email("test@example.com").role(Role.STUDENT).build();

    String token = authTokenService.genAccessToken(member);

    TokenMemberPayload payload = authTokenService.parseMemberPayload(token);

    assertThat(payload.id()).isEqualTo(1L);
    assertThat(payload.email()).isEqualTo("test@example.com");
    assertThat(payload.role()).isEqualTo("STUDENT");
  }

  @Test
  @DisplayName("ID로 RefreshToken을 생성한다")
  void givenId_whenGenRefreshToken_thenReturnValidToken() {
    Long id = 1L;

    String token = authTokenService.genRefreshToken(id);
    TokenMemberPayload payload = authTokenService.parseMemberPayload(token);

    assertThat(payload.id()).isEqualTo(1L);
    assertThat(payload.email()).isEqualTo("");
    assertThat(payload.role()).isEqualTo("");
  }

  @Test
  @DisplayName("정상 토큰을 parseMemberPayload로 복호화하면 payload를 반환한다")
  void givenValidToken_whenGetPayload_thenReturnPayload() {
    Long id = 1L;
    String token = authTokenService.genRefreshToken(id);

    TokenMemberPayload payload = authTokenService.parseMemberPayload(token);
    assertThat(payload.id()).isEqualTo(1L);
  }

  @Test
  @DisplayName("유효하지 않은 토큰은 parseMemberPayload가 null을 반환한다")
  void givenInvalidToken_whenGetPayload_thenReturnNull() {
    String invalidToken = "invalid.token.here";

    TokenMemberPayload payload = authTokenService.parseMemberPayload("invalid.token.here");
    assertThat(payload).isNull();
  }

  @DisplayName("유효한 RefreshToken이면 true를 반환한다")
  @Test
  void givenValidRefreshToken_whenValidate_thenReturnTrue() {
    Long id = 1L;
    String refreshToken = authTokenService.genRefreshToken(id);

    boolean isValid = authTokenService.validateRefreshToken(refreshToken);

    assertThat(isValid).isTrue();
  }

  @DisplayName("유효하지 않은 RefreshToken이면 false를 반환한다")
  @Test
  void givenInvalidRefreshToken_whenValidate_thenReturnFalse() {
    String invalidToken = "fake.token.value";

    boolean isValid = authTokenService.validateRefreshToken(invalidToken);

    assertThat(isValid).isFalse();
  }
}
