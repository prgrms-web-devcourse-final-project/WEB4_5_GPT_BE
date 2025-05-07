package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.TokenType;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.TokenMemberPayload;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth.InvalidAccessTokenException;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth.InvalidRefreshTokenException;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth.AccessTokenExpiredException;
import com.WEB4_5_GPT_BE.unihub.global.util.Ut;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthTokenService {

  private final String keyString;
  private final int accessTokenExpireSeconds;
  private final int refreshTokenExpireSeconds;

  public AuthTokenService(
      @Value("${custom.jwt.secret-key}") String keyString,
      @Value("${custom.jwt.access-token-expire-seconds}") int accessTokenExpireSeconds,
      @Value("${custom.jwt.refresh-token-expire-seconds}") int refreshTokenExpireSeconds) {
    this.keyString = keyString;
    this.accessTokenExpireSeconds = accessTokenExpireSeconds;
    this.refreshTokenExpireSeconds = refreshTokenExpireSeconds;
  }

  public String genAccessToken(Member member) {
    return Ut.Jwt.createToken(
        keyString,
        accessTokenExpireSeconds,
        Map.of("id", member.getId(), "email", member.getEmail(), "role", member.getRole()));
  }

  public String genRefreshToken(Long id) {
    return Ut.Jwt.createToken(keyString, refreshTokenExpireSeconds, Map.of("id", id));
  }

  public boolean validateRefreshToken(String token) {
    return Ut.Jwt.isValidToken(keyString, token);
  }

  public Long getMemberIdFromToken(String token, TokenType type) {
    validateTokenOrThrow(token, type);

    // 유효하다면 payload 파싱
    TokenMemberPayload payload = parseMemberPayload(token);
    return payload != null ? payload.id() : null;
  }

  private void validateTokenOrThrow(String token, TokenType type) {
    try {
      // 여기서 예외를 직접 던지도록 분리된 메서드 호출
      Ut.Jwt.validateToken(keyString, token);
    } catch (ExpiredJwtException e) {
      if (type == TokenType.ACCESS) {
        throw new AccessTokenExpiredException(); // 로그인 중 AccessToken 만료
      } else {
        throw new InvalidRefreshTokenException(); // RefreshToken은 만료되어도 Invalid로 처리
      }
    } catch (Exception e) {
      if (type == TokenType.ACCESS) {
        throw new InvalidAccessTokenException();
      } else {
        throw new InvalidRefreshTokenException();
      }
    }
  }

  public TokenMemberPayload parseMemberPayload(String token) {
    if (!Ut.Jwt.isValidToken(keyString, token)) return null;

    Map<String, Object> payload = Ut.Jwt.getPayload(keyString, token);
    if (payload == null) return null;

    try {
      Long id = ((Number) payload.get("id")).longValue();
      String email = (String) payload.getOrDefault("email", "");
      String role = (String) payload.getOrDefault("role", "");
      return new TokenMemberPayload(id, email, role);
    } catch (Exception e) {
      return null; // 예상치 못한 payload 형식 예외 방지
    }
  }
}
