package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.EmailCodeVerificationRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.PasswordResetConfirmationRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.ProfessorSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.StudentSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.UpdateEmailRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.UpdateMajorRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.UpdatePasswordRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.VerifyPasswordRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.MyPageProfessorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.MyPageStudentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.ProfessorCourseResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.UpdateMajorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.member.*;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage.ProfessorProfileNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage.StudentProfileNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.ProfessorProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.MajorService;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.UniversityService;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

  private final MemberRepository memberRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final ProfessorProfileRepository professorProfileRepository;
  private final UniversityService universityService;
  private final MajorService majorService;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;
  private final CourseRepository courseRepository;

  @Override
  public void signUpStudent(StudentSignUpRequest request) {

    UniversityContext universityContext = validateEmailAndLoadSchoolInfo(request.email(), request.universityId(), request.majorId());
    validateStudentSignUp(request);

    StudentProfile profile =
        StudentProfile.builder()
            .studentCode(request.studentCode())
            .university(universityContext.university())
            .major(universityContext.major())
            .grade(request.grade())
            .semester(request.semester())
            .build();

    Member member =
        Member.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .name(request.name())
            .role(Role.STUDENT)
            .studentProfile(profile)
            .build();

    profile.setMember(member);

    memberRepository.save(member);
  }

  private void validateEmailVerification(String email) {
    if (!emailService.isAlreadyVerified(email)) {
      throw new EmailNotVerifiedException();
    }
  }

  private void validateStudentSignUp(StudentSignUpRequest request) {
    boolean emailExists = memberRepository.existsByEmail(request.email());
    boolean codeExists = studentProfileRepository.existsByStudentCodeAndUniversityId(
            request.studentCode(), request.universityId()
    );

    if (emailExists || codeExists) {
      throw new EmailOrStudentCodeAlreadyExistsException();
    }
  }

  @Override
  public void signUpProfessor(ProfessorSignUpRequest request) {
    UniversityContext universityContext = validateEmailAndLoadSchoolInfo(request.email(), request.universityId(), request.majorId());
    validateProfessorSignUp(request);

    ProfessorProfile profile =
        ProfessorProfile.builder()
            .employeeId(request.employeeId())
            .university(universityContext.university())
            .major(universityContext.major())
            .approvalStatus(ApprovalStatus.PENDING)
            .build();

    Member member =
        Member.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .name(request.name())
            .role(Role.PROFESSOR)
            .professorProfile(profile)
            .build();

    profile.setMember(member);

    memberRepository.save(member);
  }

  private void validateProfessorSignUp(ProfessorSignUpRequest request) {
    boolean emailExists = memberRepository.existsByEmail(request.email());
    boolean employeeIdExists = professorProfileRepository.existsByEmployeeIdAndUniversityId(
            request.employeeId(), request.universityId()
    );

    if (emailExists || employeeIdExists) {
      throw new DuplicateProfessorSignUpInfoException();
    }
  }

  private UniversityContext validateEmailAndLoadSchoolInfo(String email, Long universityId, Long majorId) {
    University university = universityService.findUniversityById(universityId);
    Major major = majorService.getMajor(universityId, majorId);
    validateEmailVerification(email);
    return new UniversityContext(university, major);
  }

  private record UniversityContext(University university, Major major) {}

  @Override
  public void sendVerificationCode(String email) {
    if (emailService.isAlreadyVerified(email)) {
      throw new EmailAlreadyVerifiedException();
    }

    try {
      emailService.sendVerificationCode(email);
    } catch (Exception e) {
      throw new EmailSendFailureException();
    }
  }

  @Override
  public void verifyEmailCode(EmailCodeVerificationRequest request) {
    String email = request.email();
    String emailCode = request.emailCode();

    emailService.verifyCode(email, emailCode);

    emailService.markEmailAsVerified(email); // 인증 완료 표시
    emailService.deleteVerificationCode(email); // 인증코드 삭제
  }

  @Override
  public void resetPassword(PasswordResetConfirmationRequest request) {
    String email = request.email();
    String newPassword = request.password();

    Member member = memberRepository
            .findByEmail(email)
            .orElseThrow(EmailNotFoundException::new);

    if (passwordEncoder.matches(newPassword, member.getPassword())) {
      throw new PasswordSameAsOldException();
    }

    member.setPassword(passwordEncoder.encode(newPassword));
    memberRepository.save(member);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Member> findById(Long id) {
    return memberRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public MyPageStudentResponse getStudentMyPage(Long memberId) {
    Member member = findActiveMemberById(memberId);
    StudentProfile profile = studentProfileRepository.findById(memberId)
            .orElseThrow(StudentProfileNotFoundException::new);
    return MyPageStudentResponse.from(member, profile);
  }

  @Override
  @Transactional(readOnly = true)
  public MyPageProfessorResponse getProfessorMyPage(Long memberId) {
    Member member = findActiveMemberById(memberId);
    ProfessorProfile profile = professorProfileRepository.findById(memberId)
            .orElseThrow(ProfessorProfileNotFoundException::new);
    return MyPageProfessorResponse.from(member, profile);
  }

  @Override
  public List<ProfessorCourseResponse> getProfessorCourses(Long memberId) {
    Member member = findActiveMemberById(memberId);

    if (member.getRole() != Role.PROFESSOR) {
      throw new UnihubException("403", "교수만 접근할 수 있는 기능입니다.");
    }

      ProfessorProfile profile = professorProfileRepository
              .findByMemberId(memberId)
              .orElseThrow(() -> new UnihubException("404", "교수 프로필을 찾을 수 없습니다."));

      return courseRepository.findByProfessorId(profile.getId())
              .stream()
              .map(ProfessorCourseResponse::from)
              .toList();
  }

  @Override
  public void updatePassword(Long memberId, UpdatePasswordRequest request) {
    Member member = findActiveMemberById(memberId);
    if (!passwordEncoder.matches(request.password(), member.getPassword())) {
      throw new UnihubException("400", "현재 비밀번호가 일치하지 않습니다.");
    }
    member.setPassword(passwordEncoder.encode(request.newPassword()));
  }


  @Override
  public void updateEmail(Long memberId, UpdateEmailRequest request) {
    Member member = findActiveMemberById(memberId);
    if (member.getEmail().equals(request.newEmail())) {
      throw new UnihubException("400", "현재 사용 중인 이메일과 동일합니다.");
    }
    if (memberRepository.existsByEmail(request.newEmail())) {
      throw new UnihubException("409", "이미 사용 중인 이메일입니다.");
    }
    member.setEmail(request.newEmail());
  }

  @Override
  public UpdateMajorResponse updateMajor(Long memberId, UpdateMajorRequest request) {
    Member member = findActiveMemberById(memberId);

    if (member.getRole() != Role.STUDENT) {
      throw new UnihubException("403", "학생만 전공을 변경할 수 있습니다.");
    }

    StudentProfile profile = studentProfileRepository.findById(memberId)
            .orElseThrow(() -> new UnihubException("404", "학생 프로필을 찾을 수 없습니다."));
    if (profile.getMajor().getId().equals(request.majorId())) {
      throw new UnihubException("400", "현재 전공과 동일합니다.");
    }
    Major newMajor = majorService.getMajor(profile.getUniversity().getId(), request.majorId());
    profile.setMajor(newMajor);
    return UpdateMajorResponse.from(newMajor);
  }

  @Override
  public void verifyPassword(Long memberId, VerifyPasswordRequest request) {
    Member member = findActiveMemberById(memberId);
    if (!passwordEncoder.matches(request.password(), member.getPassword())) {
      throw new UnihubException("403", "비밀번호가 일치하지 않습니다.");
    }
  }

  @Override
  public void deleteMember(Long memberId) {
    Member member = findActiveMemberById(memberId);
    member.markDeleted();
  }

  @Override
  @Transactional
  public void updateAllStudentSemesters() {
      int pageSize = 500; // 적절한 배치 크기
      int page = 0;
      Pageable pageable = PageRequest.of(page, pageSize);
      Page<StudentProfile> studentPage;

      do {
          // 페이지 단위로 학생 데이터 조회
          studentPage = studentProfileRepository.findAll(pageable);
          List<StudentProfile> students = studentPage.getContent();

          log.info("학기 업데이트 배치 처리 중: 페이지 {} (총 {} 명의 학생 처리 중)", page, students.size());

          for (StudentProfile student : students) {
              // 현재 학기가 2학기(2)인 경우, 학년을 올리고 1학기로 변경
              if (student.getSemester() == 2) {
                  student.setGrade(student.getGrade() + 1);
                  student.setSemester(1);
              } else {
                  // 현재 학기가 1학기(1)인 경우, 2학기로 변경
                  student.setSemester(2);
              }
          }

          // 배치 단위로 저장
          studentProfileRepository.saveAll(students);
          pageable = PageRequest.of(++page, pageSize);

      } while (studentPage.hasNext());

      log.info("모든 학생 학기 정보 업데이트 완료. 총 {} 페이지 처리됨", page);
  }

  private Member findActiveMemberById(Long id) {
    Member member = memberRepository.findById(id)
            .orElseThrow(() -> new UnihubException("404", "회원 정보를 찾을 수 없습니다."));

    if (member.isDeleted()) {
      if (member.getDeletedAt() != null &&
              member.getDeletedAt().plusDays(30).isBefore(LocalDateTime.now())) {
        memberRepository.delete(member);
          throw new UnihubException("404", "30일이 경과하여 계정이 삭제되었습니다.");
      }
        throw new UnihubException("401", "탈퇴한 계정입니다. 30일 이내 로그인 시 복구됩니다.");
    }

    return member;
  }
}

