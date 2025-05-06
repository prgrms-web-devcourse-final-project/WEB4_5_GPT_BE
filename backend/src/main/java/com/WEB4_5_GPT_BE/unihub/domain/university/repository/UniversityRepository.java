package com.WEB4_5_GPT_BE.unihub.domain.university.repository;

import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UniversityRepository extends JpaRepository<University, Long> {

    // ub300ud559uad50 uc774ub984uc774 uc874uc7acud558ub294uc9c0 ud655uc778
    boolean existsByName(String name);
    
    // uc774uba54uc77c ub3c4uba54uc778uc774 uc874uc7acud558ub294uc9c0 ud655uc778
    boolean existsByEmailDomain(String emailDomain);

    Optional<University> findByName(String name);
}
