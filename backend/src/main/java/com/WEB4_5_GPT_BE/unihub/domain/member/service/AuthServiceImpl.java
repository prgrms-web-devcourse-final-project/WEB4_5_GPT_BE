package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.AdminLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.MemberLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.AdminLoginResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.MemberLoginResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.global.Rq;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthTokenService authTokenService;
  private final StringRedisTemplate redisTemplate;
  private final Rq rq;

  private static final String LOGIN_FAIL_PREFIX = "login:fail:";
  private static final int MAX_FAIL_COUNT = 5;
  private static final Duration LOCK_DURATION = Duration.ofMinutes(5);
  private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(7);

  @Override
  @Transactional
  public MemberLoginResponse login(MemberLoginRequest request) {
    Member member = authenticate(request.email(), request.password());
    return toMemberLoginResponse(member);
  }

  @Override
  @Transactional
  public AdminLoginResponse adminLogin(AdminLoginRequest request) {
    Member member = authenticate(request.email(), request.password());

    if (member.getRole() != Role.ADMIN) {
      throw new UnihubException("403", "관리자 권한이 없습니다.");
    }

    return toAdminLoginResponse(member);
  }

  private Member authenticate(String email, String password) {
    validateLoginFailLimit(email);

    Member member = memberRepository.findByEmail(email).orElseThrow(() -> {
      recordLoginFailure(email);
      return new UnihubException("401", "이메일 또는 비밀번호가 잘못되었습니다.");
    });

    if (!passwordEncoder.matches(password, member.getPassword())) {
      recordLoginFailure(email);
      throw new UnihubException("401", "이메일 또는 비밀번호가 잘못되었습니다.");
    }

    resetLoginFailure(email);
    return member;
  }

  private void validateLoginFailLimit(String email) {
    String key = LOGIN_FAIL_PREFIX + email;
    int failCount = Optional.ofNullable(redisTemplate.opsForValue().get(key))
            .map(Integer::parseInt).orElse(0);

    if (failCount >= MAX_FAIL_COUNT) {
      throw new UnihubException("429", "비밀번호 오류 5회 이상. 5분간 로그인이 제한됩니다.");
    }
  }

  private void recordLoginFailure(String email) {
    String key = LOGIN_FAIL_PREFIX + email;
    Long failCount = redisTemplate.opsForValue().increment(key);
    if (failCount != null && failCount == 1L) {
      redisTemplate.expire(key, LOCK_DURATION);
    }
  }

  private void resetLoginFailure(String email) {
    redisTemplate.delete(LOGIN_FAIL_PREFIX + email);
  }

  private MemberLoginResponse toMemberLoginResponse(Member member) {
    String accessToken = authTokenService.genAccessToken(member);
    String refreshToken = authTokenService.genRefreshToken(member.getId());
    saveRefreshToken(member.getId(), refreshToken);
    return new MemberLoginResponse(accessToken, refreshToken);
  }

  private AdminLoginResponse toAdminLoginResponse(Member member) {
    MemberLoginResponse base = toMemberLoginResponse(member);
    return new AdminLoginResponse(base.accessToken(), base.refreshToken());
  }

  private void saveRefreshToken(Long memberId, String refreshToken) {
    redisTemplate.opsForValue().set("refresh:" + memberId, refreshToken, REFRESH_TOKEN_DURATION);
  }

  @Override
  @Transactional
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    String accessToken = rq.getAccessToken();
    if (accessToken == null) throw new UnihubException("401", "인증이 필요합니다.");

    Long memberId = authTokenService.getMemberIdFromToken(accessToken);
    if (memberId == null) throw new UnihubException("401", "유효하지 않은 형식.");

    redisTemplate.delete("refresh:" + memberId);
    rq.removeCookie("refreshToken");
  }

  @Override
  @Transactional
  public MemberLoginResponse refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = rq.getRefreshToken();
    if (refreshToken == null) throw new UnihubException("401", "Refresh Token이 존재하지 않습니다.");
    if (!authTokenService.validateRefreshToken(refreshToken)) {
      throw new UnihubException("401", "유효하지 않은 refreshToken입니다.");
    }

    Long memberId = authTokenService.getMemberIdFromToken(refreshToken);
    if (memberId == null) throw new UnihubException("400", "잘못된 refreshToken 형식입니다.");

    String redisRefreshToken = redisTemplate.opsForValue().get("refresh:" + memberId);
    if (redisRefreshToken == null || !redisRefreshToken.equals(refreshToken)) {
      throw new UnihubException("401", "Refresh Token 정보가 일치하지 않습니다.");
    }

    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new UnihubException("404", "존재하지 않는 회원입니다."));

    String newAccessToken = authTokenService.genAccessToken(member);
    return new MemberLoginResponse(newAccessToken, null);
  }
}
