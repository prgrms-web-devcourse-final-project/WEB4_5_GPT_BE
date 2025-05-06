package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.EmailCodeVerificationRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.PasswordResetConfirmationRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.ProfessorSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.StudentSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.mypage.*;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.MyPageProfessorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.MyPageStudentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.ProfessorCourseResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.UpdateMajorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;

import java.util.List;
import java.util.Optional;

public interface MemberService {
    // 학생 회원가입
    void signUpStudent(StudentSignUpRequest request);

    // 교직원 회원가입
    void signUpProfessor(ProfessorSignUpRequest request);

    // 이메일로 인증코드 발송
    void sendVerificationCode(String email);

    // 이메일 인증 코드 검증
    void verifyEmailCode(EmailCodeVerificationRequest request);

    // 비밀번호 재설정
    void resetPassword(PasswordResetConfirmationRequest request);

    // Id를 통한 Member 찾기
    Optional<Member> findById(Long id);

    MyPageStudentResponse getStudentMyPage(Long memberId);
    MyPageProfessorResponse getProfessorMyPage(Long memberId);
    List<ProfessorCourseResponse> getProfessorCourses(Long memberId);
    void updateName(Long memberId, UpdateNameRequest request);
    void updatePassword(Long memberId, UpdatePasswordRequest request);
    void updateEmail(Long memberId, UpdateEmailRequest request);
    UpdateMajorResponse updateMajor(Long memberId, UpdateMajorRequest request);
    void verifyPassword(Long memberId, VerifyPasswordRequest request);
    void deleteMember(Long memberId);
    
    // 모든 학생의 학기와 학년 정보를 업데이트
    void updateAllStudentSemesters();
    
    // 개별 학생의 학기와 학년 정보 업데이트
    void updateStudentSemester(Long memberId, Integer grade, Integer semester);
}
