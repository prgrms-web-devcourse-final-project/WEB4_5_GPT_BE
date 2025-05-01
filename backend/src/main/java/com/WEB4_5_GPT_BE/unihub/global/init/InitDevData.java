package com.WEB4_5_GPT_BE.unihub.global.init;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.ProfessorSignupRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.StudentSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.MemberService;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.MajorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class InitDevData {

    private final UniversityRepository universityRepository;
    private final MajorRepository majorRepository;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void init() {
        if (memberRepository.count() > 0) return;

        University university = universityRepository.save(
                University.builder().name("A대학교").build());

        Major major = majorRepository.save(
                Major.builder().name("소프트웨어전공").university(university).build());

        // --- 학생 계정 생성 ---
        memberService.signUpStudent(new StudentSignUpRequest(
                "haneulkim@auni.ac.kr", "비밀번호", "김하늘", "20250001",
                university.getId(), major.getId(), 1, 1, Role.STUDENT
        ));

        // --- 교직원 계정 생성 ---
        memberService.signUpProfessor(new ProfessorSignupRequest(
                "professor@auni.ac.kr", "password", "김교수", "EMP20250001",
                university.getId(), major.getId(), Role.PROFESSOR
        ));

        // --- 관리자 계정 생성 ---
        Member admin = Member.builder()
                .email("adminmaster@auni.ac.kr")
                .password(passwordEncoder.encode("adminPw")) // 암호화 필수
                .name("최고관리자")
                .role(Role.ADMIN)
                .build();
        memberRepository.save(admin);
    }
}
