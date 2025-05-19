package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.member.enums.VerificationPurpose;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.member.EmailAlreadyVerifiedException;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
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

    /**
     * 목적별(email purpose)로 이메일 인증 코드를 생성, 저장 후 전송합니다.
     * @param email  학교 이메일
     * @param purpose  인증 목적 (가입, 비밀번호 재설정 등)
     */
    public void sendVerificationCode(String email, VerificationPurpose purpose) {

        String codeKey = buildKey(purpose, email);
        String verifiedKey = buildVerifiedKey(purpose, email);

        boolean isVerified = redisTemplate.hasKey(verifiedKey);
        boolean hasCode = redisTemplate.hasKey(codeKey);

        // 인증 완료 상태인데 코드가 없다? => 인증 완료 처리된 상태
        if (isVerified && !hasCode) {
            throw new EmailAlreadyVerifiedException();
        }

        // 그 외의 경우는 인증 중이거나 코드 만료된 경우 → 새 코드 발급
        String code = generateRandomCode();
        try {
            redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRATION);
        } catch (Exception e) {
            throw new UnihubException("500", "인증 코드 저장에 실패했습니다.");
        }

        sendEmail(email, code, purpose);
    }

    /**
     * 실제 이메일 발송 로직입니다.
     * @param code  Redis에 저장된 인증 코드
     */
    private void sendEmail(String email, String code, VerificationPurpose purpose) {
    String realEmailAddress = toRealEmailAddress(email);

    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(realEmailAddress);
    message.setFrom("UniHub <awsweb72@gmail.com>");
    message.setSubject("[UniHub] 확인 코드 안내 (" + purpose.name() + ")");
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

   /**
    * 학교 이메일을 실제 전송용 주소로 변환합니다.
    */
  private String toRealEmailAddress(String schoolEmail) {
    // ex) student@auni.ac.kr -> student@gmail.com
    String localPart = schoolEmail.split("@")[0];
    return localPart + "@gmail.com";
  }
    /**
     * 저장된 코드와 사용자가 입력한 코드를 비교하여 유효성을 검증합니다.
     * @throws UnihubException 만료 또는 불일치 시
     */
    public void verifyCode(String email, String inputCode, VerificationPurpose purpose) {
        String key       = buildKey(purpose, email);
        String savedCode = redisTemplate.opsForValue().get(key);
        if (savedCode == null) {
            throw new UnihubException("400", "인증 코드가 만료되었습니다.");
        }
        if (!savedCode.equals(inputCode)) {
            throw new UnihubException("400", "인증 코드가 일치하지 않습니다.");
        }
    }
    /**
     * 인증 완료 표시를 Redis에 남겨, 재인증 여부 체크에 사용합니다.
     */
    public void markEmailAsVerified(String email, VerificationPurpose purpose) {
        String key = buildVerifiedKey(purpose, email);
        redisTemplate.opsForValue().set(key, "true", VERIFIED_EXPIRATION);
    }
    /**
     * 이미 인증된 상태인지 확인합니다.
     */
    public boolean isAlreadyVerified(String email, VerificationPurpose purpose) {
        String verifiedKey = buildVerifiedKey(purpose, email);
        return Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey));
    }
    /**
     * 인증 코드 삭제 (검증 후) 로직
     */
    public void deleteVerificationCode(String email, VerificationPurpose purpose) {
        String key = buildKey(purpose, email);
        redisTemplate.delete(key);
    }
    /**
     * 6자리 난수 인증 코드를 생성합니다.
     */
  private String generateRandomCode() {
    Random random = new Random();
    int code = 100000 + random.nextInt(900000);
    return String.valueOf(code);
  }
    /**
     * Redis에 저장할 키를 목적별로 빌드합니다.
     */
  private String buildKey(VerificationPurpose purpose, String email) {
    return "email:" + purpose.name() + ":verification:" + email;
  }
    /**
     * 인증 완료 상태를 저장할 Redis 키를 빌드합니다.
     */
    private String buildVerifiedKey(VerificationPurpose purpose, String email) {
        return "email:" + purpose.name() + ":verified:" + email;
    }

  /**
   * 관리자 초대 이메일 전송
   */
  @Async
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
