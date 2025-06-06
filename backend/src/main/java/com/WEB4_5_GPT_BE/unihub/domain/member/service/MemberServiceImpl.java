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
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.*;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;

import com.WEB4_5_GPT_BE.unihub.domain.member.enums.VerificationPurpose;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.member.*;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage.ProfessorProfileNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage.StudentProfileNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.ProfessorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentRepository;
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
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final UniversityService universityService;
    private final MajorService majorService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CourseRepository courseRepository;

    @Override
    public void signUpStudent(StudentSignUpRequest request) {

        UniversityContext universityContext = validateEmailAndLoadSchoolInfo(request.email(), request.universityId(), request.majorId());
        validateStudentSignUp(request);

        Student profile =
                Student.builder()
                        .email(request.email())
                        .password(passwordEncoder.encode(request.password()))
                        .name(request.name())
                        .studentCode(request.studentCode())
                        .university(universityContext.university())
                        .major(universityContext.major())
                        .grade(request.grade())
                        .semester(request.semester())
                        .build();

        studentRepository.save(profile);
    }

  private void validateEmailVerification(String email,VerificationPurpose purpose) {
    if (!emailService.isAlreadyVerified(email,purpose)) {
      throw new EmailNotVerifiedException();
    }
  }

    private void validateStudentSignUp(StudentSignUpRequest request) {
        boolean emailExists = memberRepository.existsByEmail(request.email());
        boolean codeExists = studentRepository.existsByStudentCodeAndUniversityId(
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

        Professor profile =
                Professor.builder()
                        .email(request.email())
                        .password(passwordEncoder.encode(request.password()))
                        .name(request.name())
                        .employeeId(request.employeeId())
                        .university(universityContext.university())
                        .major(universityContext.major())
                        .approvalStatus(ApprovalStatus.PENDING)
                        .build();

        professorRepository.save(profile);
    }

    private void validateProfessorSignUp(ProfessorSignUpRequest request) {
        boolean emailExists = memberRepository.existsByEmail(request.email());
        boolean employeeIdExists = professorRepository.existsByEmployeeIdAndUniversityId(
                request.employeeId(), request.universityId()
        );

        if (emailExists || employeeIdExists) {
            throw new DuplicateProfessorSignUpInfoException();
        }
    }

  private UniversityContext validateEmailAndLoadSchoolInfo(String email, Long universityId, Long majorId) {
    University university = universityService.findUniversityById(universityId);
    Major major = majorService.getMajor(universityId, majorId);
    validateEmailDomainMatchesUniversity(email, university);
    validateEmailVerification(email, VerificationPurpose.SIGNUP);
    return new UniversityContext(university, major);
  }

    private void validateEmailDomainMatchesUniversity(String email, University university) {
        String[] emailParts = email.split("@");
        if (emailParts.length != 2 || !emailParts[1].equalsIgnoreCase(university.getEmailDomain())) {
            throw new InvalidEmailDomainException(university.getEmailDomain());
        }
    }

    private record UniversityContext(University university, Major major) {
    }

  @Override
  public void sendVerificationCode(String email, VerificationPurpose purpose) {
    if (emailService.isAlreadyVerified(email,purpose)) {
      throw new EmailAlreadyVerifiedException();
    }

    try {
      emailService.sendVerificationCode(email,purpose);
    } catch (Exception e) {
      throw new EmailSendFailureException();
    }
  }

  @Override
  public void verifyEmailCode(String email, String code, VerificationPurpose purpose) {
    emailService.verifyCode(email, code, purpose);

    emailService.markEmailAsVerified(email,purpose); // 인증 완료 표시
    emailService.deleteVerificationCode(email,purpose); // 인증코드 삭제
  }

    @Override
    public void resetPassword(PasswordResetConfirmationRequest request) {
        String email = request.email();
        String newPassword = request.password();

    // 이메일로 등록된 사용자 조회
    Member member = memberRepository
            .findByEmail(email)
            .orElseThrow(EmailNotFoundException::new);
      // 이메일 인증 확인
      validateEmailVerification(email,VerificationPurpose.PASSWORD_RESET);
    // 새로운 비밀번호가 기존 비밀번호와 같은지 확인
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
        Student profile = studentRepository.findById(memberId)
                .orElseThrow(StudentProfileNotFoundException::new);
        return MyPageStudentResponse.from(member, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public MyPageProfessorResponse getProfessorMyPage(Long memberId) {
        Member member = findActiveMemberById(memberId);
        Professor profile = professorRepository.findById(memberId)
                .orElseThrow(ProfessorProfileNotFoundException::new);
        return MyPageProfessorResponse.from(member, profile);
    }

    @Override
    public List<ProfessorCourseResponse> getProfessorCourses(Long memberId) {
        Member member = findActiveMemberById(memberId);

        if (member.getRole() != Role.PROFESSOR) {
            throw new UnihubException("403", "교수만 접근할 수 있는 기능입니다.");
        }

        Professor profile = professorRepository
                .findById(memberId)
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

        // 이메일이 현재와 동일한 경우
        if (member.getEmail().equals(request.newEmail())) {
            throw new UnihubException("400", "현재 사용 중인 이메일과 동일합니다.");
        }

        // 이미 사용 중인 이메일인지 확인
        if (memberRepository.existsByEmail(request.newEmail())) {
            throw new UnihubException("409", "이미 사용 중인 이메일입니다.");
        }

        // 새 이메일 인증 후 이메일 변경
        validateEmailVerification(request.newEmail(), VerificationPurpose.EMAIL_CHANGE);

        member.setEmail(request.newEmail());
        memberRepository.save(member);
    }

    @Override
    public UpdateMajorResponse updateMajor(Long memberId, UpdateMajorRequest request) {
        Member member = findActiveMemberById(memberId);

        if (member.getRole() != Role.STUDENT) {
            throw new UnihubException("403", "학생만 전공을 변경할 수 있습니다.");
        }

        Student profile = studentRepository.findById(memberId)
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
        Page<Student> studentPage;
        int totalUpdated = 0;

        do {
            // 페이지 단위로 학생 데이터 조회
            studentPage = studentRepository.findAll(pageable);
            List<Student> students = studentPage.getContent();

            log.info("학기 업데이트 배치 처리 중: 페이지 {} (총 {} 명의 학생 처리 중)", page, students.size());
            int batchUpdated = 0;

            for (Student student : students) {
                // 4학년 학생은 업데이트에서 제외
                if (student.getGrade() >= 4) {
                    continue;
                }

                // 현재 학기가 2학기(2)인 경우, 학년을 올리고 1학기로 변경
                if (student.getSemester() == 2) {
                    student.setGrade(student.getGrade() + 1);
                    student.setSemester(1);
                    batchUpdated++;
                } else {
                    // 현재 학기가 1학기(1)인 경우, 2학기로 변경
                    student.setSemester(2);
                    batchUpdated++;
                }
            }

            // 배치 단위로 저장
            studentRepository.saveAll(students);
            totalUpdated += batchUpdated;
            log.info("페이지 {} 처리 완료: {} 명의 학생 업데이트됨", page, batchUpdated);

            pageable = PageRequest.of(++page, pageSize);

        } while (studentPage.hasNext());

        log.info("모든 학생 학기 정보 업데이트 완료. 총 {} 페이지, {} 명 처리됨", page, totalUpdated);
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

    public MyPageAdminResponse getAdminMyPage(Long memberId) {
        Member member = findActiveMemberById(memberId);

        if (member.getRole() != Role.ADMIN) {
            throw new UnihubException("403", "관리자만 접근할 수 있습니다.");
        }

        return MyPageAdminResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .build();
    }
}

