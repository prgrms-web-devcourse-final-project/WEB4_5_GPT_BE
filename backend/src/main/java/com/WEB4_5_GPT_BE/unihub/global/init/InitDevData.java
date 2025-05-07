package com.WEB4_5_GPT_BE.unihub.global.init;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Profile("dev") // 개발 환경에서만 동작하는 테스트 데이터 초기화
@RequiredArgsConstructor
public class InitDevData {

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

        // 2) 수강신청 기간 테스트 데이터 생성
        helper.createEnrollmentPeriod(
                university,
                LocalDate.now().getYear(),             // 2025
                1,                                     // 1학년
                1,                                     // 1학기
                LocalDate.of(2025, 5, 1),              // 시작일
                LocalDate.of(2025, 5, 31)              // 종료일
        );

        // 2) 테스트용 강좌 생성
        List<Course> courses = initCourses(professor, major);

        // 3) “김하늘” 학생에게 두 개 강좌가 수강신청 되어있도록 설정
        Member student = helper.getMemberByEmail("haneulkim@auni.ac.kr");
        for (int i = 0; i < 2; i++) {
            helper.createEnrollment(student, courses.get(i).getId());
        }
    }

    /**
     * Major 하나에 속한 테스트용 강좌 3개를 만들고, 각 스케줄을 붙여서 반환합니다.
     */
    private List<Course> initCourses(Member professor, Major major) {

        // 자료구조
        Course dataStructure = helper.createCourse(
                "자료구조", major, "OO동 401호",
                30, 0, 3,
                professor.getProfessorProfile(),
                3, 2, "/plans/data-structure.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(dataStructure, DayOfWeek.MON, "09:00:00", "10:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(dataStructure, DayOfWeek.FRI, "14:00:00", "15:30:00");

        // 운영체제
        Course os = helper.createCourse(
                "운영체제", major, "OO동 402호",
                30, 0, 2,
                professor.getProfessorProfile(),
                3, 2, "/plans/os.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(os, DayOfWeek.TUE, "09:00:00", "10:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(os, DayOfWeek.THU, "14:00:00", "15:30:00");

        // 네트워크
        Course network = helper.createCourse(
                "네트워크", major, "OO동 403호",
                25, 0, 3,
                professor.getProfessorProfile(),
                3, 2, "/plans/network.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(network, DayOfWeek.WED, "10:00:00", "11:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(network, DayOfWeek.FRI, "16:00:00", "17:30:00");

        Course javaCourse = helper.createCourse(
                "자바 프로그래밍", major, "공학관 301호",
                40, 0, 3,
                professor.getProfessorProfile(),
                1, 1, null
        );
        helper.createCourseScheduleAndAssociateWithCourse(javaCourse, DayOfWeek.MON, "09:00", "11:00");

        return List.of(dataStructure, os, network, javaCourse);
    }
}
