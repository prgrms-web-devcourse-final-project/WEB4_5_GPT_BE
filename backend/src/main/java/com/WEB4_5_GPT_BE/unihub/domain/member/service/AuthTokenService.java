package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.member.dto.TokenMemberPayload;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.global.util.Ut;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

  public Long getMemberIdFromToken(String token) {
    TokenMemberPayload payload = parseMemberPayload(token);
    return payload != null ? payload.id() : null;
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
