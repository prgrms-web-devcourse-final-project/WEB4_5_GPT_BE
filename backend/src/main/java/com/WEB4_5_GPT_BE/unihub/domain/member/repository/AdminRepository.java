package com.WEB4_5_GPT_BE.unihub.domain.member.repository;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
}
