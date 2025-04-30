package com.WEB4_5_GPT_BE.unihub.global.security;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final MemberRepository memberRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("해당 이메일의 회원을 찾을 수 없습니다."));

    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + member.getRole().name());

    return new SecurityUser(member, Collections.singletonList(authority));
  }
}
