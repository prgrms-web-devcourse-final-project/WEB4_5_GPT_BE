package com.WEB4_5_GPT_BE.unihub.domain.member.repository;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {

    // 사번 + 대학ID로 중복 체크
    boolean existsByEmployeeIdAndUniversityId(String employeeId, Long universityId);

    @Query(
            "SELECT pp FROM Professor pp "
                    + "JOIN FETCH pp.university "
                    + "JOIN FETCH pp.major "
                    + "WHERE (:universityId IS NULL OR pp.university.id = :universityId) "
                    + "AND (:professorName IS NULL OR pp.name LIKE %:professorName%) "
                    + "AND (:majorId IS NULL OR pp.major.id = :majorId) "
                    + "AND (:status IS NULL OR pp.approvalStatus = :status)")
    Page<Professor> findProfessorsWithFilters(
        @Param("universityId") Long universityId,
        @Param("professorName") String professorName,
        @Param("majorId") Long majorId,
        @Param("status") ApprovalStatus status,
        Pageable pageable);

    Optional<Professor> findByUniversityIdAndEmployeeId(Long universityId, String employeeId);
}
