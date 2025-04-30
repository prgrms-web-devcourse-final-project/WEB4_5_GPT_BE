package com.WEB4_5_GPT_BE.unihub.domain.university.repository;

import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University, Long> {

  // 필요 시: 이름으로 검색
  boolean existsByName(String name);
}
