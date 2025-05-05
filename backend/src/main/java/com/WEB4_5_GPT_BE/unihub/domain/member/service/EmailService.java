package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;
  private final StringRedisTemplate redisTemplate;
  private static final Duration CODE_EXPIRATION = Duration.ofMinutes(5);
  private static final Duration VERIFIED_EXPIRATION =
      Duration.ofHours(1); // 인증 완료 표시 1시간 유지 (원하면 더 길게 설정 가능)
  private static final String DEFAULT_ADMIN_PASSWORD = "changeme";

  public void sendVerificationCode(String email) {
    String code = generateAndStoreCode(email);
    sendEmail(email, code);
  }

  public String generateAndStoreCode(String email) {
    String code = generateRandomCode();
    redisTemplate.opsForValue().set(buildVerificationKey(email), code, CODE_EXPIRATION);
    return code;
  }

  private void sendEmail(String email, String code) {
    String realEmailAddress = toRealEmailAddress(email);

    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(realEmailAddress);
    message.setFrom("UniHub <awsweb72@gmail.com>");
    message.setSubject("[UniHub] 확인 코드 안내");
    message.setText("""
        안녕하세요, UniHub입니다.

        요청하신 이메일 인증번호는 아래와 같습니다:

        인증번호: %s

        본 인증번호는 5분 동안 유효합니다.
        인증번호를 입력하여 인증을 완료해 주세요.

        감사합니다.
        """.formatted(code));

    mailSender.send(message);
  }

  private String toRealEmailAddress(String schoolEmail) {
    // ex) student@auni.ac.kr -> student@gmail.com
    String localPart = schoolEmail.split("@")[0];
    return localPart + "@gmail.com";
  }

  public boolean verifyCode(String email, String inputCode) {
    String savedCode = redisTemplate.opsForValue().get(buildVerificationKey(email)); // nullable: Redis 미존재 시 null

    if (savedCode == null) {
      throw new UnihubException("400", "이메일 인증 코드가 만료되었습니다.");
    }

    if (!savedCode.equals(inputCode)) {
      throw new UnihubException("400", "이메일 인증 코드가 잘못되었습니다.");
    }

    return savedCode.equals(inputCode);
  }

  public void markEmailAsVerified(String email) {
    redisTemplate.opsForValue().set(buildVerifiedKey(email), "true", VERIFIED_EXPIRATION);
  }

  public boolean isAlreadyVerified(String email) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(buildVerifiedKey(email)));
  }

  public void deleteVerificationCode(String email) {
    redisTemplate.delete(buildVerificationKey(email));
  }

  private String generateRandomCode() {
    Random random = new Random();
    int code = 100000 + random.nextInt(900000);
    return String.valueOf(code);
  }

  private String buildVerificationKey(String email) {
    return "email:verification:" + email;
  }

  private String buildVerifiedKey(String email) {
    return "email:verified:" + email;
  }
  
  /**
   * 관리자 초대 이메일 전송
   */
  public void sendAdminInvitation(String email, String adminName) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(email);
    message.setFrom("UniHub <awsweb72@gmail.com>");
    message.setSubject("[UniHub] 관리자 초대");
    message.setText("""
        안녕하세요, %s님.
        
        UniHub 관리자로 초대되었습니다.
        아래 정보로 로그인하신 후, 비밀번호를 변경해 주세요.
        
        이메일: %s
        임시 비밀번호: %s
        
        UniHub 관리자 페이지에서 다양한 관리 기능을 사용하실 수 있습니다.
        
        감사합니다.
        """.formatted(adminName, email, DEFAULT_ADMIN_PASSWORD));
        
    mailSender.send(message);
  }
}
