package com.WEB4_5_GPT_BE.unihub.domain.member.repository;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

  // 학번 + 대학ID로 중복 체크
  boolean existsByStudentCodeAndUniversityId(String studentCode, Long universityId);
}
