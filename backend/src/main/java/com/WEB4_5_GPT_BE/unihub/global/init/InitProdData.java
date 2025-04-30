package com.WEB4_5_GPT_BE.unihub.global.init;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.MemberService;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.MajorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.StudentSignUpRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("prod") // 실제 운영 환경에서만 실행
@RequiredArgsConstructor
public class InitProdData {

    private final UniversityRepository universityRepository;
    private final MajorRepository majorRepository;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final StudentProfileRepository studentProfileRepository;

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
        Member professor = Member.builder()
                .email("professor@auni.ac.kr")
                .password("password")
                .name("김교수")
                .role(Role.PROFESSOR)
                .build();
        memberRepository.save(professor);

        professor.setProfessorProfile(ProfessorProfile.builder()
                .id(professor.getId())
                .member(professor)
                .employeeId("EMP20250001")
                .university(university)
                .major(major)
                .build());

        // --- 관리자 계정 생성 ---
        Member admin = Member.builder()
                .email("adminmaster@auni.ac.kr")
                .password("adminPw")
                .name("최고관리자")
                .role(Role.ADMIN)
                .build();
        memberRepository.save(admin);
    }
}
