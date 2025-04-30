package com.WEB4_5_GPT_BE.unihub.global.security;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import java.util.Collection;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

@Getter
public class SecurityUser extends User {

  private final Long id;
  private final String name;

  public SecurityUser(Member member, Collection<? extends GrantedAuthority> authorities) {
    super(member.getEmail(), member.getPassword(), authorities);
    this.id = member.getId();
    this.name = member.getName();
  }
}
