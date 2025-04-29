package com.WEB4_5_GPT_BE.unihub.domain.admin.service;

import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.*;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.EnrollmentPeriodResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.ProfessorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.StudentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
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
    private final CourseRepository courseRepository;
    private final UniversityRepository universityRepository;

    /**
     * 학생 회원 목록 조회
     */
    public Page<StudentResponse> getStudents(StudentSearchRequest searchRequest, Pageable pageable) {
        Page<StudentProfile> students = studentProfileRepository.findStudentsWithFilters(
                searchRequest.universityId(),
                searchRequest.majorId(),
                searchRequest.grade(),
                searchRequest.semester(),
                pageable
        );

        return students.map(profile -> new StudentResponse(
                profile.getMember().getId(),
                profile.getUniversity().getName(),
                profile.getMember().getName(),
                profile.getStudentCode(),
                profile.getMajor().getName(),
                profile.getGrade(),
                profile.getSemester(),
                profile.getMember().getCreatedAt()));
    }

    /**
     * 교직원 등록 신청 조회
     */
    public Page<ProfessorResponse> getProfessors(ProfessorSearchRequest searchRequest, Pageable pageable) {
        Page<ProfessorProfile> professors = professorProfileRepository.findProfessorsWithFilters(
                searchRequest.universityId(),
                searchRequest.professorName(),
                searchRequest.majorId(),
                searchRequest.status(),
                pageable
        );

        return professors.map(profile -> new ProfessorResponse(
                profile.getMember().getId(),
                profile.getUniversity().getName(),
                profile.getMember().getName(),
                profile.getMajor().getName(),
                profile.getApprovalStatus(),
                profile.getMember().getCreatedAt()));
    }

    /**
     * 교직원 등록 상태 변경
     */
    @Transactional
    public void changeProfessorStatus(Long memberId, ProfessorApprovalRequest request) {

        ProfessorProfile professorProfile = professorProfileRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 교직원이 존재하지 않습니다."));

        professorProfile.setApprovalStatus(request.approvalStatus());
    }

    /**
     * 수강신청 기간 조회
     */
    public Page<EnrollmentPeriodResponse> getEnrollmentPeriods(EnrollmentPeriodSearchRequest searchRequest, Pageable pageable) {
        LocalDate startDateFrom = searchRequest.startDateFrom() != null ?
                LocalDate.parse(searchRequest.startDateFrom()) : null;
        LocalDate startDateTo = searchRequest.startDateTo() != null ?
                LocalDate.parse(searchRequest.startDateTo()) : null;
        LocalDate endDateFrom = searchRequest.endDateFrom() != null ?
                LocalDate.parse(searchRequest.endDateFrom()) : null;
        LocalDate endDateTo = searchRequest.endDateTo() != null ?
                LocalDate.parse(searchRequest.endDateTo()) : null;

        Page<EnrollmentPeriod> periods = courseRepository.findWithFilters(
                searchRequest.universityName(),
                startDateFrom,
                startDateTo,
                endDateFrom,
                endDateTo,
                pageable
        );

        return periods.map(period -> new EnrollmentPeriodResponse(
                period.getId(),
                period.getUniversity().getName(),
                period.getGrade(),
                period.getStartDate(),
                period.getEndDate()));
    }

    /**
     * 수강신청 기간 등록
     */
    @Transactional
    public EnrollmentPeriodResponse createEnrollmentPeriod(EnrollmentPeriodRequest request) {
        // 시작일이 종료일보다 늦은 경우 에러 발생
        LocalDate startDate = LocalDate.parse(request.startDate());
        LocalDate endDate = LocalDate.parse(request.endDate());

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("종료일자는 시작일자보다 커야합니다.");
        }

        University university = universityRepository.getReferenceById(request.universityId());
        EnrollmentPeriod enrollmentPeriod = EnrollmentPeriod.builder()
                .university(university)
                .grade(request.grade())
                .startDate(startDate)
                .endDate(endDate)
                .build();

        EnrollmentPeriod savedPeriod = courseRepository.save(enrollmentPeriod);

        return new EnrollmentPeriodResponse(
                savedPeriod.getId(),
                savedPeriod.getUniversity().getName(),
                savedPeriod.getGrade(),
                savedPeriod.getStartDate(),
                savedPeriod.getEndDate());
    }

    /**
     * 수강신청 기간 수정
     */
    @Transactional
    public EnrollmentPeriodResponse updateEnrollmentPeriod(Long periodId, EnrollmentPeriodRequest request) {
        EnrollmentPeriod enrollmentPeriod = courseRepository.findById(periodId)
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
        enrollmentPeriod.setStartDate(startDate);
        enrollmentPeriod.setEndDate(endDate);

        return new EnrollmentPeriodResponse(
                enrollmentPeriod.getId(),
                enrollmentPeriod.getUniversity().getName(),
                enrollmentPeriod.getGrade(),
                enrollmentPeriod.getStartDate(),
                enrollmentPeriod.getEndDate());
    }

    /**
     * 수강신청 기간 삭제
     */
    @Transactional
    public void deleteEnrollmentPeriod(Long periodId) {
        // 존재 여부 확인
        if (!courseRepository.existsById(periodId)) {
            throw new IllegalArgumentException("해당 수강신청 기간이 존재하지 않습니다.");
        }

        courseRepository.deleteById(periodId);
    }


    /**
     * 관리자 초대
     */
    @Transactional
    public void inviteAdmin(AdminInviteRequest request) {
        // 이미 존재하는 이메일인지 확인
        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        // 관리자 생성 및 초대 메일 발송 로직
        Member admin = Member.builder()
                .name(request.adminName())
                .email(request.email())
                .password("changeme")
                .role(Role.ADMIN)
                .build();

        memberRepository.save(admin);

        // TODO 이메일 전송
    }
}
