package com.WEB4_5_GPT_BE.unihub.global.security;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.TokenType;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth.AccessTokenExpiredException;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.AuthTokenService;
import com.WEB4_5_GPT_BE.unihub.global.Rq;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.WEB4_5_GPT_BE.unihub.global.security.SecurityConstants.AUTH_WHITELIST;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {

  private final Rq rq;
  private final AuthTokenService authTokenService;
  private final MemberRepository memberRepository;
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;

  private String extractAccessToken() {
    String authHeader = rq.getHeader("Authorization");
    if (authHeader != null && authHeader.trim().startsWith("Bearer ")) {
      return authHeader.substring("Bearer ".length()).trim();
    }
    return rq.getValueFromCookie("accessToken");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String url = request.getRequestURI();

    if (AUTH_WHITELIST.contains(url)) {
      filterChain.doFilter(request, response);
      return;
    }

    String accessToken = extractAccessToken();
    if (accessToken != null) {
      try {
        Long memberId = authTokenService.getMemberIdFromToken(accessToken, TokenType.ACCESS);
        if (memberId != null) {
          memberRepository.findById(memberId).ifPresent(rq::setLogin);
        }
      } catch (AccessTokenExpiredException e) {
        authenticationEntryPoint.commence(request, response, e);
        return;
      }
    }


    filterChain.doFilter(request, response);
  }
}
