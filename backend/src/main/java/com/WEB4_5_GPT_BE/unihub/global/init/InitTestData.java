package com.WEB4_5_GPT_BE.unihub.global.init;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.StudentSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.MemberService;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.MajorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
@RequiredArgsConstructor
public class InitTestData {

  private final UniversityRepository universityRepository;
  private final MajorRepository majorRepository;
  private final MemberService memberService;
  private final MemberRepository memberRepository;
  private final StudentProfileRepository studentProfileRepository;

  @PostConstruct
  @Transactional
  public void init() {
    studentProfileRepository.deleteAll();
    memberRepository.deleteAll();
    majorRepository.deleteAll();
    universityRepository.deleteAll();

    University university = University.builder().name("A대학교").build();
    university = universityRepository.save(university);

    Major major = Major.builder().name("소프트웨어전공").university(university).build();
    majorRepository.save(major);

    memberService.signUpStudent(
        new StudentSignUpRequest(
            "teststudent@auni.ac.kr",
            "password",
            "테스트학생",
            "20250002",
            university.getId(),
            major.getId(),
            1,
            1,
            Role.STUDENT));

    memberService.signUpStudent(
        new StudentSignUpRequest(
            "teststudent2@auni.ac.kr",
            "password",
            "테스트학생2",
            "20250003",
            university.getId(),
            major.getId(),
            1,
            1,
            Role.STUDENT));
  }
}
