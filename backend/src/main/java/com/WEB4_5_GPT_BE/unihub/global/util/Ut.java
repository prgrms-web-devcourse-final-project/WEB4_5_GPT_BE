package com.WEB4_5_GPT_BE.unihub.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

public class Ut {
  public static class Json {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toString(Object obj) {
      try {
        return objectMapper.writeValueAsString(obj);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class Jwt {
    public static String createToken(
        String keyString, int expireSeconds, Map<String, Object> claims) {

      SecretKey secretKey = Keys.hmacShaKeyFor(keyString.getBytes());

      Date issuedAt = new Date();
      Date expiration = new Date(issuedAt.getTime() + 1000L * expireSeconds);

      return Jwts.builder()
          .claims(claims)
          .issuedAt(issuedAt)
          .expiration(expiration)
          .signWith(secretKey)
          .compact();
    }

    public static boolean isValidToken(String keyString, String token) {
      try {
        // 유효한 JWT인지 여부만 확인 (만료, 서명 오류 등 포함)
        // 예외 발생 시 false 반환 (catch로 잡아서 처리)
        validateToken(keyString, token);

      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }

      return true;
    }

    public static void validateToken(String keyString, String token) {
      // 유효하지 않은 JWT인 경우 (예: 만료됨, 서명 오류 등) 예외를 그대로 throw함
      // 호출하는 쪽에서 try-catch로 직접 예외 처리해야 함
      SecretKey secretKey = Keys.hmacShaKeyFor(keyString.getBytes());
      Jwts.parser().verifyWith(secretKey).build().parse(token);
    }

    public static Map<String, Object> getPayload(String keyString, String jwtStr) {

      SecretKey secretKey = Keys.hmacShaKeyFor(keyString.getBytes());

      return (Map<String, Object>)
          Jwts.parser().verifyWith(secretKey).build().parse(jwtStr).getPayload();
    }
  }
}
