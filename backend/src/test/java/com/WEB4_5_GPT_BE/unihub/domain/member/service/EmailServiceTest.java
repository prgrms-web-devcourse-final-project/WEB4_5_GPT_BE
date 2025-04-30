package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {
  @InjectMocks private EmailService emailService;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private StringRedisTemplate redisTemplate;

  @Mock private JavaMailSender mailSender;

  @Test
  @DisplayName("인증코드를 이메일로 보내고 레디스에 저장한다")
  void givenEmail_whenSendVerificationCode_thenSendMailAndSaveToRedis() {
    // given
    String email = "test@example.com";

    // when
    emailService.sendVerificationCode(email);

    // then
    verify(mailSender).send(any(SimpleMailMessage.class));
    verify(redisTemplate.opsForValue())
        .set(eq("email:verification:" + email), anyString(), eq(Duration.ofMinutes(5)));
  }

  @Test
  @DisplayName("저장된 인증코드와 입력한 코드가 일치하면 true를 반환한다")
  void givenCorrectCode_whenVerifyCode_thenReturnTrue() {
    // given
    String email = "test@example.com";
    String code = "123456";

    when(redisTemplate.opsForValue().get("email:verification:" + email)).thenReturn(code);

    // when
    boolean result = emailService.verifyCode(email, code);

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("저장된 인증코드와 입력한 코드가 불일치하면 false를 반환한다")
  void givenIncorrectCode_whenVerifyCode_thenReturnFalse() {
    // given
    String email = "test@example.com";
    String savedCode = "123456";
    String wrongCode = "654321";

    when(redisTemplate.opsForValue().get("email:verification:" + email)).thenReturn(savedCode);

    // when
    boolean result = emailService.verifyCode(email, wrongCode);

    // then
    assertThat(result).isFalse();
  }
}
