package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.TokenType;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.AdminLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.MemberLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.AdminLoginResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.MemberLoginResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth.*;
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
  private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(1);

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
      throw new AdminPermissionDeniedException();
    }

    return toAdminLoginResponse(member);
  }

  private Member authenticate(String email, String password) {
    validateLoginFailLimit(email);

    Member member = memberRepository.findByEmail(email).orElseThrow(() -> {
      recordLoginFailure(email);
      return new InvalidCredentialException();
    });
      //[복구] 탈퇴 회원 처리
      if (member.isDeleted()) {
          if (member.getDeletedAt() != null &&
                  member.getDeletedAt().plusDays(30).isBefore(java.time.LocalDateTime.now())) {
              memberRepository.delete(member);
              throw new UnihubException("404", "30일이 지나 계정이 삭제되었습니다.");
          }

          member.setDeleted(false);
          member.setDeletedAt(null);
          memberRepository.save(member);
      }

    if (!passwordEncoder.matches(password, member.getPassword())) {
      recordLoginFailure(email);
      throw new InvalidCredentialException();
    }

    if (member.getRole() == Role.PROFESSOR) {
      if (member.getProfessorProfile() == null || member.getProfessorProfile().getApprovalStatus() != ApprovalStatus.APPROVED) {
        throw new ProfessorNotApprovedException();
      }
    }

    resetLoginFailure(email);
    return member;
  }

  private void validateLoginFailLimit(String email) {
    String key = LOGIN_FAIL_PREFIX + email;
    int failCount = Optional.ofNullable(redisTemplate.opsForValue().get(key))
            .map(Integer::parseInt).orElse(0);

    if (failCount >= MAX_FAIL_COUNT) {
      throw new LoginLockedException();
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
    rq.addCookie("refreshToken", refreshToken, REFRESH_TOKEN_DURATION);
    return new MemberLoginResponse(accessToken, member.getId(), member.getEmail(), member.getRole());
  }

  private AdminLoginResponse toAdminLoginResponse(Member member) {
    MemberLoginResponse base = toMemberLoginResponse(member);
    return new AdminLoginResponse(base.accessToken(), member.getId(), member.getEmail(), member.getRole());
  }

  private void saveRefreshToken(Long memberId, String refreshToken) {
    redisTemplate.opsForValue().set("refresh:" + memberId, refreshToken, REFRESH_TOKEN_DURATION);
  }

  @Override
  @Transactional
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    String accessToken = rq.getAccessToken();
    if (accessToken == null) throw new AccessTokenNotFoundException();

    Long memberId = authTokenService.getMemberIdFromToken(accessToken, TokenType.ACCESS);
    if (memberId == null) throw new InvalidAccessTokenException();

    redisTemplate.delete("refresh:" + memberId);
    rq.removeCookie("refreshToken");
  }

  @Override
  @Transactional
  public MemberLoginResponse refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = rq.getRefreshToken();
    if (refreshToken == null) throw new RefreshTokenNotFoundException();
    if (!authTokenService.validateRefreshToken(refreshToken)) {
      throw new InvalidRefreshTokenException();
    }

    Long memberId = authTokenService.getMemberIdFromToken(refreshToken, TokenType.REFRESH);
    if (memberId == null) throw new InvalidRefreshTokenFormatException();

    String redisRefreshToken = redisTemplate.opsForValue().get("refresh:" + memberId);
    if (redisRefreshToken == null || !redisRefreshToken.equals(refreshToken)) {
      throw new RefreshTokenMismatchException();
    }

    Member member = memberRepository.findById(memberId)
            .orElseThrow(MemberNotFoundException::new);

    String newAccessToken = authTokenService.genAccessToken(member);
    return new MemberLoginResponse(newAccessToken, member.getId(), member.getEmail(), member.getRole());
  }
}
