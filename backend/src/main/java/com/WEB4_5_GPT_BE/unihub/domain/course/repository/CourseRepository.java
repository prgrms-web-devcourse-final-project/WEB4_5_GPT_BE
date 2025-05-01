package com.WEB4_5_GPT_BE.unihub.domain.course.repository;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<EnrollmentPeriod, Long> {

  @Query(
      "SELECT e FROM EnrollmentPeriod e "
          + "WHERE (:universityName IS NULL OR e.university.name = :universityName) "
          + "AND (:startDateFrom IS NULL OR e.startDate >= :startDateFrom) "
          + "AND (:startDateTo IS NULL OR e.startDate <= :startDateTo) "
          + "AND (:endDateFrom IS NULL OR e.endDate >= :endDateFrom) "
          + "AND (:endDateTo IS NULL OR e.endDate <= :endDateTo)")
  Page<EnrollmentPeriod> findWithFilters(
      @Param("universityName") String universityName,
      @Param("startDateFrom") LocalDate startDateFrom,
      @Param("startDateTo") LocalDate startDateTo,
      @Param("endDateFrom") LocalDate endDateFrom,
      @Param("endDateTo") LocalDate endDateTo,
      Pageable pageable);
}
