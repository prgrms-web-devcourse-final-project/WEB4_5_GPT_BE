package com.WEB4_5_GPT_BE.unihub.global.init;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"stg", "prod"}) // 테스트, 운영 서버 환경에서만 동작하는 테스트 데이터 초기화
@RequiredArgsConstructor
public class InitProdOrStgData {

    private final InitDataHelper helper;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void init() {
        if (helper.countMembers() > 0) {
            helper.ensureProfessorApprovalStatus("professor@auni.ac.kr", ApprovalStatus.APPROVED);
            return;
        }

        University university = helper.createUniversity("A대학교", "auni.ac.kr");
        Major major = helper.createMajor("소프트웨어전공", university);
        Major major2 = helper.createMajor("컴퓨터공학전공", university);

        helper.createStudent("haneulkim@auni.ac.kr", "studentPw", "김하늘", "20250001",
                university.getId(), major.getId());

        Member professor = helper.createProfessor("professor@auni.ac.kr", "password", "김교수", "EMP20250001",
                university.getId(), major.getId(), ApprovalStatus.APPROVED);

        helper.createAdmin("adminmaster@auni.ac.kr", "adminPw", "최고관리자", passwordEncoder);

        Course javaCourse = helper.createCourse(
                "자바 프로그래밍",      // title
                major,                // 전공
                "공학관 301호",         // location
                40, 0, 3,             // capacity, enrolled, credit
                professor.getProfessorProfile(),
                1, 1,                 // grade, semester
                null                  // 첨부 파일 경로
        );


        helper.createCourseScheduleAndAssociateWithCourse(
                javaCourse,
                DayOfWeek.MON,
                "09:00",
                "11:00"
        );
    }
}
