package com.WEB4_5_GPT_BE.unihub.domain.university.repository;

import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UniversityRepository extends JpaRepository<University, Long> {

    // 대학교 이름이 존재하는지 확인
    boolean existsByName(String name);
    
    // 이메일 도메인이 존재하는지 확인
    boolean existsByEmailDomain(String emailDomain);

    Optional<University> findByName(String name);
}
