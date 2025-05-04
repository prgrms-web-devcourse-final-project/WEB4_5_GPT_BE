package com.WEB4_5_GPT_BE.unihub.global.init;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("test") // 테스트 환경에서만 동작하는 테스트 데이터 초기화
@RequiredArgsConstructor
public class InitTestData {

    private final InitDataHelper helper;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void init() {
        // 테스트 환경에서는 항상 초기화
        studentProfileRepository.deleteAll();
        helper.clearAllMemberData();

        University university = helper.createUniversity("A대학교");
        Major major = helper.createMajor("소프트웨어전공", university);
        Major major2 = helper.createMajor("컴퓨터공학전공", university);

        helper.createStudent("teststudent@auni.ac.kr", "password", "테스트학생", "20250002",
                university.getId(), major.getId());

        helper.createStudent("teststudent2@auni.ac.kr", "password", "테스트학생2", "20250003",
                university.getId(), major.getId());

        // 승인된 교수 (로그인 성공용)
        Member authenticatedProfessor = helper.createProfessor("professor@auni.ac.kr", "password", "김교수", "EMP00001",
                university.getId(), major.getId(), ApprovalStatus.APPROVED);

        // 승인 안 된 교수 (로그인 실패용)
        helper.createProfessor("pending@auni.ac.kr", "password", "대기중교수", "EMP00002",
                university.getId(), major.getId(), ApprovalStatus.PENDING);

        Course course = helper.createCourse("컴파일러", major, "공학관A", 200, 0, 4,
                authenticatedProfessor.getProfessorProfile(), 4, 1, "/somePath/someAttachment.jpg");

        for(DayOfWeek day : List.of(DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED)) {
            helper.createCourseScheduleAndAssociateWithCourse(course, day, "12:00", "14:00");
        }

        helper.createAdmin("adminmaster@auni.ac.kr", "adminPw", "관리자", passwordEncoder); // 인코딩 생략
    }
}
