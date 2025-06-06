package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.EmailCodeVerificationRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.PasswordResetConfirmationRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.ProfessorSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.StudentSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.UpdateEmailRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.UpdateMajorRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.UpdatePasswordRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.VerifyPasswordRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.MyPageAdminResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Admin;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.enums.VerificationPurpose;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.member.EmailNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.member.exception.member.EmailNotVerifiedException;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.ProfessorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.MajorService;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.UniversityService;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private UniversityService universityService;

    @Mock
    private MajorService majorService;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Mock
    private EmailService emailService;

    private University mockUniversity;
    private Major mockMajor;

    @BeforeEach
    void setUpCommonMocks() {
        mockUniversity = University.builder()
                .id(1L)
                .name("A대학교")
                .emailDomain("auni.ac.kr")
                .build();

        mockMajor = Major.builder()
                .id(1L)
                .name("소프트웨어전공")
                .university(mockUniversity)
                .build();
    }

    @DisplayName("학생 회원가입에 성공한다")
    @Test
    void givenValidStudentSignUpRequest_whenSignUpStudent_thenMemberSaved() {
        // given
        StudentSignUpRequest request =
                new StudentSignUpRequest(
                        "student@auni.ac.kr", "password", "홍길동", "20240001", 1L, 1L, 1, 1, Role.STUDENT);
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;

        when(emailService.isAlreadyVerified(request.email(),purpose)).thenReturn(true);
        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(studentRepository.existsByStudentCodeAndUniversityId(
                request.studentCode(), request.universityId()))
                .thenReturn(false);
        when(universityService.findUniversityById(request.universityId())).thenReturn(mockUniversity);
        when(majorService.getMajor(request.universityId(), request.majorId())).thenReturn(mockMajor);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

        // when
        memberService.signUpStudent(request);

        // then
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @DisplayName("이메일 인증이 안 되었을 때 회원가입에 실패한다")
    @Test
    void givenUnverifiedEmail_whenSignUpStudent_thenThrowUnihubException() {
        // given
        StudentSignUpRequest request = new StudentSignUpRequest(
                "student@auni.ac.kr", "password", "홍길동", "20240001", 1L, 1L, 1, 1, Role.STUDENT);
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;
        when(universityService.findUniversityById(1L)).thenReturn(mockUniversity);
        when(majorService.getMajor(1L, 1L)).thenReturn(mockMajor);
        when(emailService.isAlreadyVerified(request.email(),purpose)).thenReturn(false); // 인증 안 됨

        // when / then
        assertThatThrownBy(() -> memberService.signUpStudent(request))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("이메일 인증을 완료해주세요.");
    }

    @DisplayName("이메일이 중복되면 학생 회원가입에 실패한다")
    @Test
    void givenDuplicatedEmail_whenSignUpStudent_thenThrowUnihubException() {
        // given
        StudentSignUpRequest request = new StudentSignUpRequest(
                "student@auni.ac.kr", "password", "홍길동", "20240001", 1L, 1L, 1, 1, Role.STUDENT);
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;
        when(universityService.findUniversityById(1L)).thenReturn(mockUniversity);
        when(majorService.getMajor(1L, 1L)).thenReturn(mockMajor);
        when(emailService.isAlreadyVerified(request.email(),purpose)).thenReturn(true); // 인증 됨
        when(memberRepository.existsByEmail(request.email())).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> memberService.signUpStudent(request))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("이메일 또는 학번이 이미 등록되어 있습니다.");
    }

    @DisplayName("존재하지 않는 대학 ID로 학생 회원가입 시 실패한다")
    @Test
    void givenInvalidUniversityId_whenSignUpStudent_thenThrowUnihubException() {
        // given
        long invalidUniversityId = 9999L;
        StudentSignUpRequest request = new StudentSignUpRequest(
                "student@auni.ac.kr", "password", "홍길동", "20240001", invalidUniversityId, 1L, 1, 1, Role.STUDENT);
        
        when(universityService.findUniversityById(eq(invalidUniversityId)))
                .thenThrow(new UnihubException("404", "해당 대학이 존재하지 않습니다."));

        // when / then
        assertThatThrownBy(() -> memberService.signUpStudent(request))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("해당 대학이 존재하지 않습니다.");
    }

    @DisplayName("존재하지 않는 전공 ID로 학생 회원가입 시 실패한다")
    @Test
    void givenInvalidMajorId_whenSignUpStudent_thenThrowUnihubException() {
        // given
        StudentSignUpRequest request =
                new StudentSignUpRequest(
                        "student@auni.ac.kr", "password", "홍길동", "20240001", 1L, 9999L, 1, 1, Role.STUDENT);

        University university = University.builder().id(1L).name("테스트대학").build();

        when(universityService.findUniversityById(request.universityId())).thenReturn(university);
        when(majorService.getMajor(request.universityId(), request.majorId()))
                .thenThrow(new UnihubException("404", "존재하지 않는 전공입니다."));

        // when / then
        assertThatThrownBy(() -> memberService.signUpStudent(request))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("존재하지 않는 전공입니다.");
    }

    @DisplayName("교직원 회원가입에 성공한다")
    @Test
    void givenValidProfessorSignUpRequest_whenSignUpProfessor_thenMemberSaved() {
        // given
        ProfessorSignUpRequest request =
                new ProfessorSignUpRequest(
                        "professor@auni.ac.kr", "password", "김교수", "EMP20240001", 1L, 1L, Role.PROFESSOR);
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;
        when(emailService.isAlreadyVerified(request.email(),purpose)).thenReturn(true);
        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(professorRepository.existsByEmployeeIdAndUniversityId(
                request.employeeId(), request.universityId()))
                .thenReturn(false);
        when(universityService.findUniversityById(request.universityId())).thenReturn(mockUniversity);
        when(majorService.getMajor(request.universityId(), request.majorId())).thenReturn(mockMajor);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

        // when
        memberService.signUpProfessor(request);

        // then
        verify(professorRepository, times(1)).save(any(Professor.class));
    }

    @DisplayName("교직원 회원가입 시 approvalStatus는 기본값으로 PENDING이다")
    @Test
    void givenValidProfessorSignUpRequest_whenSignUpProfessor_thenApprovalStatusIsPending() {
        // given
        ProfessorSignUpRequest request =
                new ProfessorSignUpRequest(
                        "professor@auni.ac.kr", "password", "김교수", "EMP20240001", 1L, 1L, Role.PROFESSOR);
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;
        when(emailService.isAlreadyVerified(request.email(),purpose)).thenReturn(true);
        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(professorRepository.existsByEmployeeIdAndUniversityId(
                request.employeeId(), request.universityId())).thenReturn(false);
        when(universityService.findUniversityById(request.universityId())).thenReturn(mockUniversity);
        when(majorService.getMajor(request.universityId(), request.majorId())).thenReturn(mockMajor);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

        // 캡처용 ArgumentCaptor 추가
        ArgumentCaptor<Professor> captor = ArgumentCaptor.forClass(Professor.class);

        // when
        memberService.signUpProfessor(request);

        // then
        verify(professorRepository).save(captor.capture());
        Professor saved = captor.getValue();

        assertThat(saved.getApprovalStatus())
                .isEqualTo(com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus.PENDING);
    }

    @DisplayName("이메일 인증이 안 되었으면 교직원 회원가입에 실패한다")
    @Test
    void givenUnverifiedEmail_whenSignUpProfessor_thenThrowUnihubException() {
        // given
        ProfessorSignUpRequest request = new ProfessorSignUpRequest(
                "professor@auni.ac.kr", "password", "김교수", "EMP20240001", 1L, 1L, Role.PROFESSOR);
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;
        when(universityService.findUniversityById(1L)).thenReturn(mockUniversity);
        when(majorService.getMajor(1L, 1L)).thenReturn(mockMajor);
        when(emailService.isAlreadyVerified(request.email(),purpose)).thenReturn(false); // 인증 안 됨

        // when / then
        assertThatThrownBy(() -> memberService.signUpProfessor(request))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("이메일 인증을 완료해주세요.");
    }

    @DisplayName("이메일이 중복되면 교직원 회원가입에 실패한다")
    @Test
    void givenDuplicatedEmail_whenSignUpProfessor_thenThrowUnihubException() {
        // given
        ProfessorSignUpRequest request = new ProfessorSignUpRequest(
                "professor@auni.ac.kr", "password", "김교수", "EMP20240001", 1L, 1L, Role.PROFESSOR);
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;
        when(universityService.findUniversityById(1L)).thenReturn(mockUniversity);
        when(majorService.getMajor(1L, 1L)).thenReturn(mockMajor);
        when(emailService.isAlreadyVerified(request.email(),purpose)).thenReturn(true);
        when(memberRepository.existsByEmail(request.email())).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> memberService.signUpProfessor(request))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("이메일 또는 사번이 이미 등록되어 있습니다.");
    }

    @Test
    @DisplayName("이메일 인증 요청 시 sendVerificationCode를 호출한다")
    void givenEmail_whenSendVerificationCode_thenInvokeEmailService() {
        // given
        String email = "test@auni.ac.kr";
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;
        // when
        memberService.sendVerificationCode(email,purpose);

        // then
        verify(emailService).sendVerificationCode(email,purpose);
    }

    @DisplayName("이메일 인증에 성공하면 이메일을 인증 완료로 표시하고 인증코드를 삭제한다")
    @Test
    void givenValidVerificationCode_whenVerifyEmailCode_thenMarkVerifiedAndDeleteCode() {
        // given
        String email = "test@auni.ac.kr";
        String code = "123456";
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;
        EmailCodeVerificationRequest request = new EmailCodeVerificationRequest(email, code);

        doNothing().when(emailService).verifyCode(email, code, purpose);

        // when
        memberService.verifyEmailCode(request.email(), request.emailCode(), purpose);

        // then
        verify(emailService).markEmailAsVerified(email,purpose);
        verify(emailService).deleteVerificationCode(email,purpose);
    }

    @DisplayName("이메일 인증에 실패하면 예외를 던진다")
    @Test
    void givenInvalidVerificationCode_whenVerifyEmailCode_thenThrowUnihubException() {
        // given
        String email = "test@auni.ac.kr";
        String wrongCode = "654321";
        VerificationPurpose purpose = VerificationPurpose.SIGNUP;
        EmailCodeVerificationRequest request = new EmailCodeVerificationRequest(email, wrongCode);

        doThrow(new UnihubException("400", "이메일 인증 코드가 잘못되었습니다."))
                .when(emailService).verifyCode(email, wrongCode,purpose);

        // when / then
        assertThatThrownBy(() -> memberService.verifyEmailCode(request.email(), request.emailCode(), purpose))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("이메일 인증 코드가 잘못되었습니다.");
    }

    @DisplayName("비밀번호 재설정에 성공한다")
    @Test
    void givenValidRequest_whenResetPassword_thenPasswordUpdated() {
        // given
        String email = "test@auni.ac.kr";
        String newPassword = "newPassword123";

        Admin member =
                Admin.builder()
                        .email(email)
                        .password("encodedOldPassword")
                        .name("홍길동")
                        .build();

        // 이메일 인증을 완료한 것으로 시뮬레이션
        when(emailService.isAlreadyVerified(email, VerificationPurpose.PASSWORD_RESET)).thenReturn(true);

        when(memberRepository.findByEmail(email)).thenReturn(java.util.Optional.of(member));
        when(passwordEncoder.matches(newPassword, member.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");

        PasswordResetConfirmationRequest request =
                new PasswordResetConfirmationRequest(email, newPassword);

        // when
        memberService.resetPassword(request);

        // then
        verify(memberRepository).save(member);
    }

    @DisplayName("등록되지 않은 이메일로 비밀번호 재설정 시 예외를 발생시킨다")
    @Test
    void givenNonExistentEmail_whenResetPassword_thenThrowUnihubException() {
        // given
        String email = "nonexistent@auni.ac.kr";
        String newPassword = "newPassword123";

        when(memberRepository.findByEmail(email)).thenReturn(java.util.Optional.empty()); // 이메일이 등록되지 않음

        PasswordResetConfirmationRequest request =
                new PasswordResetConfirmationRequest(email, newPassword);

        // when / then
        assertThatThrownBy(() -> memberService.resetPassword(request))
                .isInstanceOf(EmailNotFoundException.class)  // EmailNotFoundException을 기대
                .hasMessageContaining("등록되지 않은 이메일 주소입니다.");
    }

    @DisplayName("기존 비밀번호와 동일할 경우 비밀번호 재설정에 실패한다")
    @Test
    void givenSamePassword_whenResetPassword_thenThrowUnihubException() {
        // given
        String email = "test@auni.ac.kr";
        String samePassword = "samePassword123";

        Admin member =
                Admin.builder()
                        .email(email)
                        .password("encodedSamePassword")
                        .name("홍길동")
                        .build();

        // 이메일 인증 상태 설정 (인증 완료된 상태로 가정)
        when(emailService.isAlreadyVerified(email, VerificationPurpose.PASSWORD_RESET)).thenReturn(true);

        when(memberRepository.findByEmail(email)).thenReturn(java.util.Optional.of(member));
        when(passwordEncoder.matches(samePassword, member.getPassword())).thenReturn(true);

        PasswordResetConfirmationRequest request =
                new PasswordResetConfirmationRequest(email, samePassword);

        // when / then
        assertThatThrownBy(() -> memberService.resetPassword(request))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("기존 비밀번호와 동일한 비밀번호로는 변경할 수 없습니다.");
    }

    @DisplayName("이메일 인증이 안 되었을 때 비밀번호 재설정 실패")
    @Test
    void givenUnverifiedEmail_whenResetPassword_thenThrowEmailNotVerifiedException() {
        // given
        String email = "unverified@auni.ac.kr";
        String newPassword = "newPassword123";


        Member member = Student.builder()
                .email(email)
                .password("encodedOldPassword")
                .name("홍길동")
                .role(Role.STUDENT)
                .build();

        // 이메일 인증 안된 상태로 설정
        when(emailService.isAlreadyVerified(email, VerificationPurpose.PASSWORD_RESET)).thenReturn(false);  // 인증 안됨
        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));

        PasswordResetConfirmationRequest request = new PasswordResetConfirmationRequest(email, newPassword);

        // when / then
        assertThatThrownBy(() -> memberService.resetPassword(request))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("이메일 인증을 완료해주세요.");
    }

    @DisplayName("이메일 변경에 성공한다")
    @Test
    void givenUniqueEmail_whenUpdateEmail_thenEmailUpdated() {
        // given
        Admin member = Admin.builder().id(1L).email("old@email.com").build();

        // 새 이메일 인증 완료된 상태로 설정
        given(emailService.isAlreadyVerified("new@email.com", VerificationPurpose.EMAIL_CHANGE)).willReturn(true); // 새 이메일 인증 완료 설정

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByEmail("new@email.com")).willReturn(false);

        // when
        memberService.updateEmail(1L, new UpdateEmailRequest("new@email.com"));

        // then
        assertThat(member.getEmail()).isEqualTo("new@email.com");
    }

    @DisplayName("이메일이 이미 등록되어 있을 경우 이메일 변경 실패")
    @Test
    void givenExistingEmail_whenUpdateEmail_thenThrowEmailAlreadyExistsException() {
        // given
        Admin member = Admin.builder().id(1L).email("old@email.com").build();
        String newEmail = "existing@auni.ac.kr";

        // 기존 이메일이 이미 등록되어 있다고 가정
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByEmail(newEmail)).thenReturn(true);  // 이미 존재하는 이메일

        // when / then
        assertThatThrownBy(() -> memberService.updateEmail(1L, new UpdateEmailRequest(newEmail)))
                .isInstanceOf(UnihubException.class)  // UnihubException을 기대
                .hasMessageContaining("이미 사용 중인 이메일입니다.");  // 해당 메시지를 포함해야 함
    }

    @DisplayName("전공 변경에 성공한다")
    @Test
    void givenValidMajorId_whenUpdateMajor_thenMajorIsUpdated() {
        // given
        University university = University.builder()
                .id(1L)
                .name("테스트대학교")
                .build();

        Major oldMajor = Major.builder().id(1L).name("컴퓨터공학").university(university).build();
        Major newMajor = Major.builder().id(2L).name("전자공학").university(university).build();

        Student profile = Student.builder()
                .id(1L)
                .isDeleted(false)
                .major(oldMajor)
                .university(university) // ✅ 여기가 null이면 안 됨
                .role(Role.STUDENT)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(profile));
        given(studentRepository.findById(1L)).willReturn(Optional.of(profile));
        given(majorService.getMajor(1L, 2L)).willReturn(newMajor);

        // when
        memberService.updateMajor(1L, new UpdateMajorRequest(2L));

        // then
        assertThat(profile.getMajor().getName()).isEqualTo("전자공학");
    }


    @DisplayName("비밀번호 검증에 실패한다")
    @Test
    void givenWrongPassword_whenVerifyPassword_thenThrowException() {
        // given
        Admin member = Admin.builder().id(1L).password("encodedPass").build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong", "encodedPass")).willReturn(false);

        // when / then
        assertThatThrownBy(() -> memberService.verifyPassword(1L, new VerifyPasswordRequest("wrong")))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다.");

    }
    @DisplayName("비밀번호 검증에 성공한다")
    @Test
    void givenCorrectPassword_whenVerifyPassword_thenSuccess() {
        // given

        Admin member = Admin.builder().id(1L).password("encodedPass").build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("correct", "encodedPass")).willReturn(true);  // 비밀번호 일치

        // when
        memberService.verifyPassword(1L, new VerifyPasswordRequest("correct"));

        // then
        // 비밀번호가 일치하면 예외가 발생하지 않고 정상적으로 처리되므로, 별도의 검증 없이 끝납니다.
    }

    @DisplayName("관리자 마이페이지 조회에 성공한다")
    @Test
    void givenAdminRole_whenGetAdminMyPage_thenReturnAdminInfo() {
        // given
        Admin admin = Admin.builder()
                .id(1L)
                .email("admin@unihub.com")
                .name("관리자")
                .role(Role.ADMIN)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(admin));

        // when
        MyPageAdminResponse response = memberService.getAdminMyPage(1L);

        // then
        assertThat(response.email()).isEqualTo("admin@unihub.com");
        assertThat(response.name()).isEqualTo("관리자");
        assertThat(response.role()).isEqualTo(Role.ADMIN);
    }

    @DisplayName("관리자 권한이 아닌 사용자가 관리자 마이페이지 조회 시 실패한다")
    @Test
    void givenNonAdminRole_whenGetAdminMyPage_thenThrowException() {
        // given
        Student member = Student.builder()
                .id(2L)
                .email("student@unihub.com")
                .name("학생")
                .role(Role.STUDENT)
                .isDeleted(false)
                .build();

        when(memberRepository.findById(2L)).thenReturn(Optional.of(member));

        // when / then
        assertThatThrownBy(() -> memberService.getAdminMyPage(2L))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("관리자만 접근할 수 있습니다.");
    }
    @DisplayName("관리자 비밀번호 변경에 성공한다")
    @Test
    void givenCorrectCurrentPassword_whenUpdatePassword_thenPasswordIsUpdated() {
        // given
        Admin admin = Admin.builder().id(1L).password("encodedOld").build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("oldPassword", "encodedOld")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNew");

        UpdatePasswordRequest request = new UpdatePasswordRequest("oldPassword", "newPassword");

        // when
        memberService.updatePassword(1L, request);

        // then
        assertThat(admin.getPassword()).isEqualTo("encodedNew");
    }
    @DisplayName("관리자 회원 탈퇴에 성공한다")
    @Test
    void givenAdmin_whenDeleteMember_thenMemberIsMarkedDeleted() {
        // given
        Admin admin = Admin.builder()
                .id(1L)
                .isDeleted(false)
                .build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(admin));

        // when
        memberService.deleteMember(1L);

        // then
        assertThat(admin.isDeleted()).isTrue();
        assertThat(admin.getDeletedAt()).isNotNull();
    }


}
