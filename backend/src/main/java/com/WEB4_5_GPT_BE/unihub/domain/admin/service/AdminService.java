package com.WEB4_5_GPT_BE.unihub.domain.admin.service;

import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.*;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.EnrollmentPeriodResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.ProfessorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.StudentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.EnrollmentPeriodRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.ProfessorProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminService {

  private final MemberRepository memberRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final ProfessorProfileRepository professorProfileRepository;
  private final EnrollmentPeriodRepository enrollmentPeriodRepository;
  private final UniversityRepository universityRepository;

  /** 학생 회원 목록 조회 */
  public Page<StudentResponse> getStudents(StudentSearchRequest searchRequest, Pageable pageable) {
    Page<StudentProfile> students =
        studentProfileRepository.findStudentsWithFilters(
            searchRequest.universityId(),
            searchRequest.majorId(),
            searchRequest.grade(),
            searchRequest.semester(),
            pageable);

    return students.map(
        profile ->
            new StudentResponse(
                profile.getMember().getId(),
                profile.getUniversity().getName(),
                profile.getMember().getName(),
                profile.getStudentCode(),
                profile.getMajor().getName(),
                profile.getGrade(),
                profile.getSemester(),
                profile.getMember().getCreatedAt()));
  }

  /** 교직원 등록 신청 조회 */
  public Page<ProfessorResponse> getProfessors(
      ProfessorSearchRequest searchRequest, Pageable pageable) {
    Page<ProfessorProfile> professors =
        professorProfileRepository.findProfessorsWithFilters(
            searchRequest.universityId(),
            searchRequest.professorName(),
            searchRequest.majorId(),
            searchRequest.status(),
            pageable);

    return professors.map(
        profile ->
            new ProfessorResponse(
                profile.getMember().getId(),
                profile.getUniversity().getName(),
                profile.getMember().getName(),
                profile.getMajor().getName(),
                profile.getApprovalStatus(),
                profile.getMember().getCreatedAt()));
  }

  /** 교직원 등록 상태 변경 */
  @Transactional
  public void changeProfessorStatus(Long memberId, ProfessorApprovalRequest request) {

    ProfessorProfile professorProfile =
        professorProfileRepository
            .findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("해당 교직원이 존재하지 않습니다."));

    professorProfile.setApprovalStatus(request.approvalStatus());
  }

  /** 수강신청 기간 조회 */
  public Page<EnrollmentPeriodResponse> getEnrollmentPeriods(
      EnrollmentPeriodSearchRequest searchRequest, Pageable pageable) {
    LocalDate startDateFrom =
        searchRequest.startDateFrom() != null
            ? LocalDate.parse(searchRequest.startDateFrom())
            : null;
    LocalDate startDateTo =
        searchRequest.startDateTo() != null ? LocalDate.parse(searchRequest.startDateTo()) : null;
    LocalDate endDateFrom =
        searchRequest.endDateFrom() != null ? LocalDate.parse(searchRequest.endDateFrom()) : null;
    LocalDate endDateTo =
        searchRequest.endDateTo() != null ? LocalDate.parse(searchRequest.endDateTo()) : null;

    Page<EnrollmentPeriod> periods =
        enrollmentPeriodRepository.findWithFilters(
            searchRequest.universityName(),
            startDateFrom,
            startDateTo,
            endDateFrom,
            endDateTo,
            pageable);

    return periods.map(
        period ->
            new EnrollmentPeriodResponse(
                    period.getId(),
                    period.getUniversity().getName(),
                    period.getYear(),
                    period.getGrade(),
                    period.getSemester(),
                    period.getStartDate(),
                    period.getEndDate()));
  }

  /** 수강신청 기간 등록 */
  @Transactional
  public EnrollmentPeriodResponse createEnrollmentPeriod(EnrollmentPeriodRequest request) {
      // 시작일이 종료일보다 늦은 경우 에러 발생
      LocalDate startDate = LocalDate.parse(request.startDate());
      LocalDate endDate = LocalDate.parse(request.endDate());

      if (startDate.isAfter(endDate)) {
          throw new IllegalArgumentException("종료일자는 시작일자보다 커야합니다.");
      }

      University university = universityRepository.getReferenceById(request.universityId());

      // 해당 대학, 학년, 연도, 학기에 이미 등록된 수강신청 기간이 있는지 확인
      boolean exists = enrollmentPeriodRepository.existsByUniversityIdAndGradeAndYearAndSemester(
              university.getId(),
              request.grade(),
              request.year(),
              request.semester()
      );

      if (exists) {
          throw new IllegalArgumentException(
                  String.format("%d년 %d학년 %d학기 수강신청 기간이 이미 등록되어 있습니다.",
                          request.year(), request.grade(), request.semester())
          );
      }

      EnrollmentPeriod enrollmentPeriod =
              EnrollmentPeriod.builder()
                      .university(university)
                      .year(request.year())      // 연도 추가
                      .grade(request.grade())
                      .semester(request.semester()) // 학기 설정
                      .startDate(startDate)
                      .endDate(endDate)
                      .build();

    EnrollmentPeriod savedPeriod = enrollmentPeriodRepository.save(enrollmentPeriod);

    return new EnrollmentPeriodResponse(
            savedPeriod.getId(),
            savedPeriod.getUniversity().getName(),
            savedPeriod.getYear(),         // 연도 추가
            savedPeriod.getGrade(),
            savedPeriod.getSemester(),
            savedPeriod.getStartDate(),
            savedPeriod.getEndDate());
  }

  /** 수강신청 기간 수정 */
  @Transactional
  public EnrollmentPeriodResponse updateEnrollmentPeriod(
      Long periodId, EnrollmentPeriodRequest request) {
    EnrollmentPeriod enrollmentPeriod =
            enrollmentPeriodRepository
            .findById(periodId)
            .orElseThrow(() -> new IllegalArgumentException("해당 수강신청 기간이 존재하지 않습니다."));

    // 시작일이 종료일보다 늦은 경우 에러 발생
    LocalDate startDate = LocalDate.parse(request.startDate());
      LocalDate endDate = LocalDate.parse(request.endDate());

      if (startDate.isAfter(endDate)) {
          throw new IllegalArgumentException("종료일자는 시작일자보다 커야합니다.");
      }

      University university = universityRepository.getReferenceById(request.universityId());

      enrollmentPeriod.setUniversity(university);
      enrollmentPeriod.setGrade(request.grade());
      enrollmentPeriod.setYear(request.year());
      enrollmentPeriod.setSemester(request.semester());
      enrollmentPeriod.setStartDate(startDate);
      enrollmentPeriod.setEndDate(endDate);

      return new EnrollmentPeriodResponse(
              enrollmentPeriod.getId(),
              enrollmentPeriod.getUniversity().getName(),
              enrollmentPeriod.getYear(),
              enrollmentPeriod.getGrade(),
              enrollmentPeriod.getSemester(),
              enrollmentPeriod.getStartDate(),
              enrollmentPeriod.getEndDate());
  }

  /** 수강신청 기간 삭제 */
  @Transactional
  public void deleteEnrollmentPeriod(Long periodId) {
    // 존재 여부 확인
    if (!enrollmentPeriodRepository.existsById(periodId)) {
      throw new IllegalArgumentException("해당 수강신청 기간이 존재하지 않습니다.");
    }

      enrollmentPeriodRepository.deleteById(periodId);
  }

  /** 관리자 초대 */
  @Transactional
  public void inviteAdmin(AdminInviteRequest request) {
    // 이미 존재하는 이메일인지 확인
    if (memberRepository.existsByEmail(request.email())) {
      throw new IllegalArgumentException("이미 등록된 이메일입니다.");
    }

    // 관리자 생성 및 초대 메일 발송 로직
    Member admin =
        Member.builder()
            .name(request.adminName())
            .email(request.email())
            .password("changeme")
            .role(Role.ADMIN)
            .build();

    memberRepository.save(admin);

    // TODO 이메일 전송
  }
}
