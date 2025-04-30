package com.WEB4_5_GPT_BE.unihub.domain.member.repository;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorProfileRepository extends JpaRepository<ProfessorProfile, Long> {
  // 사번 + 대학ID로 중복 체크
  boolean existsByEmployeeIdAndUniversityId(String employeeId, Long universityId);
}
