package com.WEB4_5_GPT_BE.unihub.domain.university.repository;

import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MajorRepository extends JpaRepository<Major, Long> {

  Optional<Major> findByIdAndUniversityId(Long majorId, Long universityId);
}
