package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.member.enums.VerificationPurpose;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.member.EmailAlreadyVerifiedException;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    VerificationPurpose purpose = VerificationPurpose.SIGNUP;
    // when
    emailService.sendVerificationCode(email,purpose);

    // then
    verify(mailSender).send(any(SimpleMailMessage.class));
    verify(redisTemplate.opsForValue())
        .set(eq("email:SIGNUP:verification:" + email), anyString(), eq(Duration.ofMinutes(5)));
  }

  @Test
  @DisplayName("저장된 인증코드와 입력한 코드가 일치하면 예외 없이 성공")
  void givenCorrectCode_whenVerifyCode_thenReturnTrue() {
    // given
    String email = "test@example.com";
    String code = "123456";
    VerificationPurpose purpose = VerificationPurpose.SIGNUP;

    when(redisTemplate.opsForValue().get("email:SIGNUP:verification:" + email)).thenReturn(code);

    // when & then
    emailService.verifyCode(email, code,purpose);
  }

  @Test
  @DisplayName("저장된 인증코드와 입력한 코드가 불일치하면 false를 반환한다")
  void givenIncorrectCode_whenVerifyCode_thenReturnFalse() {
    // given
    String email = "test@example.com";
    String savedCode = "123456";
    String wrongCode = "654321";
    VerificationPurpose purpose = VerificationPurpose.SIGNUP;
    when(redisTemplate.opsForValue().get("email:SIGNUP:verification:" + email)).thenReturn(savedCode);

    // when & then
    assertThatThrownBy(() -> emailService.verifyCode(email, wrongCode,purpose))
            .isInstanceOf(UnihubException.class)
            .hasMessage("인증 코드가 일치하지 않습니다.");
  }
    @Test
    @DisplayName("인증완료 O + 코드 O → 덮어쓰기 후 재전송 성공")
    void givenVerifiedAndCodeExists_whenSendVerificationCode_thenOverwriteCodeAndSendEmail() {
        String email = "test@example.com";
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;

        when(redisTemplate.hasKey("email:SIGNUP:verified:" + email)).thenReturn(true);
        when(redisTemplate.hasKey("email:SIGNUP:verification:" + email)).thenReturn(true);

        emailService.sendVerificationCode(email, purpose);

        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(redisTemplate.opsForValue())
                .set(eq("email:SIGNUP:verification:" + email), anyString(), eq(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("인증완료 O + 코드 X → 예외 발생")
    void givenVerifiedAndCodeMissing_whenSendVerificationCode_thenThrowEmailAlreadyVerified() {
        String email = "test@example.com";
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;

        when(redisTemplate.hasKey("email:SIGNUP:verified:" + email)).thenReturn(true);
        when(redisTemplate.hasKey("email:SIGNUP:verification:" + email)).thenReturn(false);

        assertThatThrownBy(() -> emailService.sendVerificationCode(email, purpose))
                .isInstanceOf(EmailAlreadyVerifiedException.class);
    }

    @Test
    @DisplayName("인증완료 X + 코드 O → 덮어쓰기 후 이메일 재전송")
    void givenUnverifiedAndCodeExists_whenSendVerificationCode_thenOverwriteCodeAndSendEmail() {
        String email = "test@example.com";
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;

        when(redisTemplate.hasKey("email:SIGNUP:verified:" + email)).thenReturn(false);
        when(redisTemplate.hasKey("email:SIGNUP:verification:" + email)).thenReturn(true);

        emailService.sendVerificationCode(email, purpose);

        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(redisTemplate.opsForValue())
                .set(eq("email:SIGNUP:verification:" + email), anyString(), eq(Duration.ofMinutes(5)));
    }

}
