package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.EmailCodeVerificationRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.PasswordResetConfirmationRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.ProfessorSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.StudentSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.UpdateEmailRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.UpdateMajorRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.VerifyPasswordRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.enums.VerificationPurpose;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.ProfessorProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentProfileRepository;
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
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private ProfessorProfileRepository professorProfileRepository;

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
        when(studentProfileRepository.existsByStudentCodeAndUniversityId(
                request.studentCode(), request.universityId()))
                .thenReturn(false);
        when(universityService.findUniversityById(request.universityId())).thenReturn(mockUniversity);
        when(majorService.getMajor(request.universityId(), request.majorId())).thenReturn(mockMajor);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

        // when
        memberService.signUpStudent(request);

        // then
        verify(memberRepository, times(1)).save(any(Member.class));
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
        when(professorProfileRepository.existsByEmployeeIdAndUniversityId(
                request.employeeId(), request.universityId()))
                .thenReturn(false);
        when(universityService.findUniversityById(request.universityId())).thenReturn(mockUniversity);
        when(majorService.getMajor(request.universityId(), request.majorId())).thenReturn(mockMajor);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

        // when
        memberService.signUpProfessor(request);

        // then
        verify(memberRepository, times(1)).save(any(Member.class));
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
        when(professorProfileRepository.existsByEmployeeIdAndUniversityId(
                request.employeeId(), request.universityId())).thenReturn(false);
        when(universityService.findUniversityById(request.universityId())).thenReturn(mockUniversity);
        when(majorService.getMajor(request.universityId(), request.majorId())).thenReturn(mockMajor);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

        // 캡처용 ArgumentCaptor 추가
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);

        // when
        memberService.signUpProfessor(request);

        // then
        verify(memberRepository).save(captor.capture());
        Member saved = captor.getValue();

        assertThat(saved.getProfessorProfile().getApprovalStatus())
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

        Member member =
                Member.builder()
                        .email(email)
                        .password("encodedOldPassword")
                        .name("홍길동")
                        .role(Role.STUDENT)
                        .build();

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

        when(memberRepository.findByEmail(email)).thenReturn(java.util.Optional.empty());

        PasswordResetConfirmationRequest request =
                new PasswordResetConfirmationRequest(email, newPassword);

        // when / then
        assertThatThrownBy(() -> memberService.resetPassword(request))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("등록되지 않은 이메일 주소입니다.");
    }

    @DisplayName("기존 비밀번호와 동일할 경우 비밀번호 재설정에 실패한다")
    @Test
    void givenSamePassword_whenResetPassword_thenThrowUnihubException() {
        // given
        String email = "test@auni.ac.kr";
        String samePassword = "samePassword123";

        Member member =
                Member.builder()
                        .email(email)
                        .password("encodedSamePassword")
                        .name("홍길동")
                        .role(Role.STUDENT)
                        .build();

        when(memberRepository.findByEmail(email)).thenReturn(java.util.Optional.of(member));
        when(passwordEncoder.matches(samePassword, member.getPassword())).thenReturn(true);

        PasswordResetConfirmationRequest request =
                new PasswordResetConfirmationRequest(email, samePassword);

        // when / then
        assertThatThrownBy(() -> memberService.resetPassword(request))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("기존 비밀번호와 동일한 비밀번호로는 변경할 수 없습니다.");
    }

    @DisplayName("이메일 변경에 성공한다")
    @Test
    void givenUniqueEmail_whenUpdateEmail_thenEmailUpdated() {
        // given
        Member member = Member.builder().id(1L).email("old@email.com").build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByEmail("new@email.com")).willReturn(false);

        // when
        memberService.updateEmail(1L, new UpdateEmailRequest("new@email.com"));

        // then
        assertThat(member.getEmail()).isEqualTo("new@email.com");
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

        StudentProfile profile = StudentProfile.builder()
                .id(1L)
                .major(oldMajor)
                .university(university) // ✅ 여기가 null이면 안 됨
                .build();

        Member member = Member.builder()
                .id(1L)
                .isDeleted(false)
                .role(Role.STUDENT)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(studentProfileRepository.findById(1L)).willReturn(Optional.of(profile));
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
        Member member = Member.builder().id(1L).password("encodedPass").build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong", "encodedPass")).willReturn(false);

        // when / then
        assertThatThrownBy(() -> memberService.verifyPassword(1L, new VerifyPasswordRequest("wrong")))
                .isInstanceOf(UnihubException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다.");

    }
}
