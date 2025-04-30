package com.WEB4_5_GPT_BE.unihub.domain.member.repository;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
  boolean existsByEmail(String email);
}
