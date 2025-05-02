package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.EmailCodeVerificationRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.PasswordResetConfirmationRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.ProfessorSignupRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.StudentSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.*;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.MyPageProfessorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.MyPageStudentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.ProfessorCourseResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.UpdateMajorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.ProfessorProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.MajorService;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.UniversityService;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
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

  @Override
  public void signUpStudent(StudentSignUpRequest request) {
      University university = universityService.findUniversityById(request.universityId());
      Major major = majorService.getMajor(request.universityId(), request.majorId());

    validateStudentSignUp(request);

    StudentProfile profile =
        StudentProfile.builder()
            .studentCode(request.studentCode())
            .university(university)
            .major(major)
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

    private void validateStudentSignUp(StudentSignUpRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new UnihubException("409", "이메일 또는 학번이 이미 등록되어 있습니다.");
        }

        if (studentProfileRepository.existsByStudentCodeAndUniversityId(
                request.studentCode(), request.universityId())) {
            throw new UnihubException("409", "이메일 또는 학번이 이미 등록되어 있습니다.");
        }
    }

    @Override
    public void signUpProfessor(ProfessorSignupRequest request) {
        University university = universityService.findUniversityById(request.universityId());
        Major major = majorService.getMajor(request.universityId(), request.majorId());

        validateProfessorSignUp(request);

        ProfessorProfile profile =
                ProfessorProfile.builder()
                        .employeeId(request.employeeId())
                        .university(university)
                        .major(major)
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

    private void validateProfessorSignUp(ProfessorSignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new UnihubException("409", "이메일 또는 사번이 이미 등록되어 있습니다.");
        }

        if (professorProfileRepository.existsByEmployeeIdAndUniversityId(
                request.employeeId(), request.universityId())) {
            throw new UnihubException("409", "이메일 또는 사번이 이미 등록되어 있습니다.");
        }
    }

  @Override
  public void sendVerificationCode(String email) {
    if (emailService.isAlreadyVerified(email)) {
      throw new UnihubException("400", "이메일은 이미 인증되었습니다.");
    }

    try {
      emailService.sendVerificationCode(email);
    } catch (Exception e) {
      throw new UnihubException("500", "이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
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

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new UnihubException("404", "등록되지 않은 이메일 주소입니다."));

    if (passwordEncoder.matches(newPassword, member.getPassword())) {
      throw new UnihubException("400", "기존 비밀번호와 동일한 비밀번호로는 변경할 수 없습니다.");
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
            .orElseThrow(() -> new UnihubException("404", "학생 프로필을 찾을 수 없습니다."));
    return MyPageStudentResponse.from(member, profile);
  }

  @Override
  @Transactional(readOnly = true)
  public MyPageProfessorResponse getProfessorMyPage(Long memberId) {
    Member member = findActiveMemberById(memberId);
    ProfessorProfile profile = professorProfileRepository.findById(memberId)
            .orElseThrow(() -> new UnihubException("404", "교수 프로필을 찾을 수 없습니다."));
    return MyPageProfessorResponse.from(member, profile);
  }

  @Override
  public List<ProfessorCourseResponse> getProfessorCourses(Long memberId) {
    Member member = findActiveMemberById(memberId);

    if (member.getRole() != Role.PROFESSOR) {
      throw new UnihubException("403", "교수만 접근할 수 있는 기능입니다.");
    }
    // TODO: 실제 강의 목록 조회 로직 작성 예정
    return Collections.emptyList(); // TODO: 강의 도메인 연동 필요

  }

  @Override
  public void updateName(Long memberId, UpdateNameRequest request) {
    findActiveMemberById(memberId).setName(request.name());
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

