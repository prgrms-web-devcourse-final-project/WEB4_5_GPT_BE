package com.WEB4_5_GPT_BE.unihub.global.init;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

        University university = helper.createUniversity("A대학교", "auni.ac.kr");
        Major major = helper.createMajor("소프트웨어전공", university);
        Major major2 = helper.createMajor("컴퓨터공학전공", university);

        helper.createStudent("teststudent@auni.ac.kr", "password", "테스트학생", "20250002",
                university.getId(), major.getId());

        helper.createStudent("teststudent2@auni.ac.kr", "password", "테스트학생2", "20250003",
                university.getId(), major.getId());

        helper.create2ndStudent("teststudent3@auni.ac.kr", "password", "테스트2학년학생1", "20250004",
                university.getId(), major.getId());

        // 승인된 교수 (로그인 성공용)
        Member authenticatedProfessor = helper.createProfessor("professor@auni.ac.kr", "password", "김교수", "EMP00001",
                university.getId(), major.getId(), ApprovalStatus.APPROVED);

        // 승인 안 된 교수 (로그인 실패용)
        helper.createProfessor("pending@auni.ac.kr", "password", "대기중교수", "EMP00002",
                university.getId(), major.getId(), ApprovalStatus.PENDING);

        Course course = helper.createCourse("컴파일러", major, "공학관A", 200, 0, 4,
                authenticatedProfessor.getProfessorProfile(), 4, 1, "/somePath/someAttachment.jpg");

        for (DayOfWeek day : List.of(DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED)) {
            helper.createCourseScheduleAndAssociateWithCourse(course, day, "12:00", "14:00");
        }

        helper.createAdmin("adminmaster@auni.ac.kr", "adminPw", "관리자", passwordEncoder);

        // 수강신청 기간 설정 (1학년·1학기)
        int year = LocalDate.now().getYear();
        helper.createEnrollmentPeriod(
                university,
                year,              // 연도
                1,                 // 학년
                1,                 // 학기
                LocalDate.of(year, 5, 1),
                LocalDate.of(year, 5, 31)
        );

        // 2-2) 테스트용 강좌 3개 생성
        List<Course> courses = initTestCourses(major, authenticatedProfessor.getProfessorProfile());

        // 2-3) 기존 생성되어있던 테스트학생에 2개의 수강신청 정보를 등록함
        Member student = helper.getMemberByEmail("teststudent@auni.ac.kr");
        helper.createEnrollment(student, courses.get(0).getId());
        helper.createEnrollment(student, courses.get(1).getId());

        // 2-3) 예외 검증용 강좌
        //  정원초과 강좌
        Course full = helper.createCourse("정원초과강좌", major, "OO동 104호",
                1, 1, 3, authenticatedProfessor.getProfessorProfile(),
                1, 1, "/plans/full.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(full, DayOfWeek.FRI, "09:00:00", "10:00:00");

        //  학점초과 강좌(20학점짜리 강좌, 신청 시 초과)
        Course heavy = helper.createCourse("학점초과강좌", major, "OO동 105호",
                30, 0, 20, authenticatedProfessor.getProfessorProfile(),
                1, 1, "/plans/heavy.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(heavy, DayOfWeek.THU, "13:00:00", "15:00:00");

        //  시간표충돌 강좌
        Course conflict = helper.createCourse("충돌강좌", major, "OO동 106호",
                30, 0, 3, authenticatedProfessor.getProfessorProfile(),
                1, 1, "/plans/conflict.pdf"
        );
        // 자료구조, 운영체제 수업과 시간표가 겹침
        helper.createCourseScheduleAndAssociateWithCourse(conflict, DayOfWeek.MON, "10:00:00", "11:00:00");
        helper.createCourseScheduleAndAssociateWithCourse(conflict, DayOfWeek.TUE, "10:00:00", "11:00:00");
    }

    /**
     * Major + ProfessorProfile 기반으로 3개의 테스트용 강좌 생성
     */
    private List<Course> initTestCourses(Major major, ProfessorProfile prof) {
        Course c1 = helper.createCourse("자료구조", major, "OO동 101호", 30, 0, 3, prof, 1, 1, "/plans/ds.pdf");
        helper.createCourseScheduleAndAssociateWithCourse(c1, DayOfWeek.MON, "09:00:00", "10:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(c1, DayOfWeek.WED, "11:00:00", "12:30:00");

        Course c2 = helper.createCourse("운영체제", major, "OO동 102호", 30, 0, 4, prof, 1, 1, "/plans/os.pdf");
        helper.createCourseScheduleAndAssociateWithCourse(c2, DayOfWeek.TUE, "09:00:00", "10:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(c2, DayOfWeek.THU, "11:00:00", "12:30:00");

        Course c3 = helper.createCourse("네트워크", major, "OO동 103호", 20, 0, 3, prof, 1, 1, "/plans/net.pdf");
        helper.createCourseScheduleAndAssociateWithCourse(c3, DayOfWeek.TUE, "14:00:00", "15:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(c3, DayOfWeek.FRI, "16:00:00", "17:30:00");

        return List.of(c1, c2, c3);
    }
}