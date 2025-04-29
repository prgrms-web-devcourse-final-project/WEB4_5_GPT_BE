package com.WEB4_5_GPT_BE.unihub.domain.member.repository;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfessorProfileRepository extends JpaRepository<ProfessorProfile, Long> {

  @Query(
      "SELECT pp FROM ProfessorProfile pp "
          + "JOIN pp.member m "
          + "WHERE m.role = 'PROFESSOR' "
          + "AND (:universityId IS NULL OR pp.university.id = :universityId) "
          + "AND (:professorName IS NULL OR m.name LIKE %:professorName%) "
          + "AND (:majorId IS NULL OR pp.major.id = :majorId) "
          + "AND (:status IS NULL OR pp.approvalStatus = :status)")
  Page<ProfessorProfile> findProfessorsWithFilters(
      @Param("universityId") Long universityId,
      @Param("professorName") String professorName,
      @Param("majorId") Long majorId,
      @Param("status") ApprovalStatus status,
      Pageable pageable);
}
