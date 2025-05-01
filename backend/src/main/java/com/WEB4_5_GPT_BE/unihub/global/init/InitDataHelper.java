package com.WEB4_5_GPT_BE.unihub.global.init;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.ProfessorSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.StudentSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.EmailService;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.MemberService;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.MajorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InitDataHelper {

    private final UniversityRepository universityRepository;
    private final MajorRepository majorRepository;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final EmailService emailService;

    public University createUniversity(String name) {
        return universityRepository.save(University.builder().name(name).build());
    }

    public Major createMajor(String name, University university) {
        return majorRepository.save(Major.builder().name(name).university(university).build());
    }

    public void createStudent(String email, String pw, String name, String studentCode, Long univId, Long majorId) {
        emailService.markEmailAsVerified(email);
        memberService.signUpStudent(new StudentSignUpRequest(email, pw, name, studentCode, univId, majorId, 1, 1, Role.STUDENT));
    }

    public void createProfessor(String email, String pw, String name, String empCode, Long univId, Long majorId, ApprovalStatus status) {
        emailService.markEmailAsVerified(email);
        memberService.signUpProfessor(new ProfessorSignUpRequest(email, pw, name, empCode, univId, majorId, Role.PROFESSOR));
        Member professor = memberRepository.findByEmail(email).orElseThrow();
        professor.getProfessorProfile().setApprovalStatus(status);
        memberRepository.save(professor);
    }

    public void createAdmin(String email, String pw, String name, PasswordEncoder encoder) {
        Member admin = Member.builder()
                .email(email)
                .password(encoder.encode(pw))
                .name(name)
                .role(Role.ADMIN)
                .build();
        memberRepository.save(admin);
    }

    public long countMembers() {
        return memberRepository.count();
    }

    public void clearAllMemberData() {
        memberRepository.deleteAll();
        majorRepository.deleteAll();
        universityRepository.deleteAll();
    }

    public void ensureProfessorApprovalStatus(String email, ApprovalStatus status) {
        Optional<Member> optionalMember = memberRepository.findByEmail(email);

        if (optionalMember.isEmpty()) return;

        Member member = optionalMember.get();

        if (member.getRole() != Role.PROFESSOR) return;

        ProfessorProfile profile = member.getProfessorProfile();
        if (profile != null && profile.getApprovalStatus() == null) {
            profile.setApprovalStatus(status);
            memberRepository.save(member);
        }
    }
}
