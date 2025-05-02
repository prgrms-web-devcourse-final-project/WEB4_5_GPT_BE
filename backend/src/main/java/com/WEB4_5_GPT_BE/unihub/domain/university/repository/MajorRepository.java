package com.WEB4_5_GPT_BE.unihub.domain.university.repository;

import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {

  // 대학별 전공 목록 조회 (리스트)
  List<Major> findByUniversity(University university);

  // 대학과 전공 이름으로 존재 여부 확인
  boolean existsByUniversityAndName(University university, String name);

  Optional<Major> findByIdAndUniversityId(Long majorId, Long universityId);
}
