package com.WEB4_5_GPT_BE.unihub.global;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.MemberService;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

@Component
@RequiredArgsConstructor
@RequestScope
public class Rq {

  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final MemberService memberService;

  public void setLogin(Member member) {
    List<SimpleGrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));

    UserDetails user = new SecurityUser(member, authorities);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
  }

  public Member getActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser)) {
      throw new UnihubException("401", "로그인이 필요합니다.");
    }

    SecurityUser user = (SecurityUser) authentication.getPrincipal();

    return Member.builder().id(user.getId()).email(user.getUsername()).name(user.getName()).build();
  }

  public Member getRealActor(Member actor) {
    return memberService
        .findById(actor.getId())
        .orElseThrow(() -> new UnihubException("404", "존재하지 않는 사용자입니다."));
  }

  public String getAccessToken() {
    String authorization = getHeader("Authorization");
    if (authorization != null && authorization.startsWith("Bearer ")) {
      return authorization.substring(7);
    }
    return null;
  }

  public String getRefreshToken() {
    return getValueFromCookie("refreshToken");
  }

  public String getHeader(String name) {
    return request.getHeader(name);
  }

  public String getValueFromCookie(String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return null;

    for (Cookie cookie : cookies) {
      if (cookie.getName().equals(name)) {
        return cookie.getValue();
      }
    }

    return null;
  }

  public void setHeader(String name, String value) {
    response.setHeader(name, value);
  }

  public void addCookie(String name, String value) {
    Cookie cookie = new Cookie(name, value);
    // cookie.setDomain("localhost");
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setAttribute("SameSite", "None");

    response.addCookie(cookie);
  }

  public void removeCookie(String name) {
    Cookie cookie = new Cookie(name, null);
    // cookie.setDomain("localhost");
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setAttribute("SameSite", "None");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }
}
