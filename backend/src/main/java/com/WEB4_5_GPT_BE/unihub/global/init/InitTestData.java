package com.WEB4_5_GPT_BE.unihub.global.init;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("test") // 테스트 환경에서만 동작하는 테스트 데이터 초기화
@RequiredArgsConstructor
public class InitTestData {

    private final InitDataHelper helper;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void init() {
        // 테스트 환경에서는 항상 초기화
        studentRepository.deleteAll();
        helper.clearAllMemberData();

        // 관리자 계정 생성 메서드
        initAdmin();

        List<University> universities = initUniversity();
        University university = universities.get(0); // A대학교

        List<Major> majors = initMajor(university);
        Major major = majors.get(0); // 소프트웨어전공

        // 학생 생성 메서드
        initStudent(university, major);

        // 교수 생성 메서드
        List<Member> professors = initProfessor(university, major);
        Member authenticatedProfessor = professors.get(0); // 승인된 교수

        // 수강신청 기간 생성
        initEnrollmentPeriod(university);

        // 테스트용 강좌 생성
        List<Course> courses = initTestCourses(major, (Professor) authenticatedProfessor);

        // 예외상황 강좌 생성
        initConflictCourses(major, (Professor) authenticatedProfessor);

        // ------- ↓초기화 이후 추가적인 작업↓ -------

        Course compiler = courses.getFirst(); // 컴파일러

        for (DayOfWeek day : List.of(DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED)) {
            helper.createCourseScheduleAndAssociateWithCourse(compiler, day, "12:00", "14:00");
        }

        // 기존 생성되어있던 테스트학생에 2개의 수강신청 정보를 등록함
        Member student = helper.getMemberByEmail("teststudent@auni.ac.kr");
        helper.createEnrollment(student, courses.get(1).getId()); // 자료구조
        helper.createEnrollment(student, courses.get(2).getId()); // 운영체제

        //공지사항 초기 데이터 추가
        helper.createNotice("필독 공지", "수강신청 일정 공지",null);

        // 초기 시간표 생성
        initTimeTable(student);
    }

    /**
     * 대학 생성 메서드
     */
    private List<University> initUniversity() {
        List<University> universities = new ArrayList<>();

        University uni1 = helper.createUniversity("A대학교", "auni.ac.kr");
        universities.add(uni1);

        return universities;
    }

    /**
     * 전공 생성 메서드
     */
    private List<Major> initMajor(University university) {
        List<Major> majors = new ArrayList<>();

        Major major1 = helper.createMajor("소프트웨어전공", university);
        majors.add(major1);
        Major major2 = helper.createMajor("컴퓨터공학전공", university);
        majors.add(major2);

        return majors;
    }

    /**
     * 학생 계정 생성 메서드
     */
    private void initStudent(University university, Major major) {
        // 1학년 학생 생성1
        helper.createStudent("teststudent@auni.ac.kr", "password", "테스트학생", "20250002",
                university.getId(), major.getId(), 1, 1);

        // 1학년 학생 생성2
        helper.createStudent("teststudent2@auni.ac.kr", "password", "테스트학생2", "20250003",
                university.getId(), major.getId(), 1, 1);

        // 2학년 학생 생성
        helper.createStudent("teststudent3@auni.ac.kr", "password", "테스트2학년학생1", "25020001",
                university.getId(), major.getId(), 2, 1);

        // 3학년 학생 생성
        helper.createStudent("test3rdstudent@auni.ac.kr", "password", "테스트3학년학생1", "25030001",
                university.getId(), major.getId(), 3, 1);

        // 4학년 학생 생성
        helper.createStudent("test4thstudent@auni.ac.kr", "password", "테스트4학년학생1", "25040001",
                university.getId(), major.getId(), 4, 1);
    }

    /**
     * 교수 계정 생성 메서드
     */
    private List<Member> initProfessor(University university, Major major) {

        List<Member> professors = new ArrayList<>();

        // 승인된 교수 (로그인 성공용)
        Member approvedProfessor = helper.createProfessor("professor@auni.ac.kr", "password", "김교수", "EMP00001",
                university.getId(), major.getId(), ApprovalStatus.APPROVED);
        professors.add(approvedProfessor);

        // 승인 안 된 교수 (로그인 실패용)
        Member pendingProfessor = helper.createProfessor("pending@auni.ac.kr", "password", "대기중교수", "EMP00002",
                university.getId(), major.getId(), ApprovalStatus.PENDING);
        professors.add(pendingProfessor);

        return professors;
    }

    /**
     * 관리자 계정 생성 메서드
     */
    private void initAdmin() {
        helper.createAdmin("adminmaster@auni.ac.kr", "adminPw", "관리자", passwordEncoder);
    }

    /**
     * 수강신청 기간을 생성하는 메서드
     */
    private void initEnrollmentPeriod(University university) {
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

        // 수강신청 기간 설정 (2학년·1학기) 없음

        // 수강신청 기간 설정 (3학년·1학기, 아직 기간 안됨)
        helper.createEnrollmentPeriod(
                university,
                year,              // 연도
                3,                 // 학년
                1,                 // 학기
                LocalDate.of(year, 6, 1),
                LocalDate.of(year, 6, 30)
        );

        // 수강신청 기간 설정 (4학년·1학기, 기간지남)
        helper.createEnrollmentPeriod(
                university,
                year,              // 연도
                4,                 // 학년
                1,                 // 학기
                LocalDate.of(year, 5, 1),
                LocalDate.of(year, 5, 9)
        );
    }

    /**
     * Major + ProfessorProfile 기반으로 3개의 테스트용 강좌 생성
     */
    private List<Course> initTestCourses(Major major, Professor prof) {

        List<Course> courses = new ArrayList<>();

        Course c1 = helper.createCourse("컴파일러", major, "공학관A", 200, 0, 4,
                prof, 4, 1, "/somePath/someAttachment.jpg");
        courses.add(c1);

        Course c2 = helper.createCourse("자료구조", major, "OO동 101호", 30, 0, 3, prof, 1, 1, "/plans/ds.pdf");
        helper.createCourseScheduleAndAssociateWithCourse(c2, DayOfWeek.MON, "09:00:00", "10:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(c2, DayOfWeek.WED, "11:00:00", "12:30:00");
        courses.add(c2);

        Course c3 = helper.createCourse("운영체제", major, "OO동 102호", 30, 0, 4, prof, 1, 1, "/plans/os.pdf");
        helper.createCourseScheduleAndAssociateWithCourse(c3, DayOfWeek.TUE, "09:00:00", "10:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(c3, DayOfWeek.THU, "11:00:00", "12:30:00");
        courses.add(c3);

        Course c4 = helper.createCourse("네트워크", major, "OO동 103호", 20, 0, 3, prof, 1, 1, "/plans/net.pdf");
        helper.createCourseScheduleAndAssociateWithCourse(c4, DayOfWeek.TUE, "14:00:00", "15:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(c4, DayOfWeek.FRI, "16:00:00", "17:30:00");
        courses.add(c4);

        return courses;
    }

    /**
     * 정원초과, 학점초과, 시간표충돌 강좌 생성 메서드
     */
    private void initConflictCourses(Major major, Professor professorProfile) {
        //  정원초과 강좌
        Course full = helper.createCourse("정원초과강좌", major, "OO동 104호",
                1, 1, 3, professorProfile,
                1, 1, "/plans/full.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(full, DayOfWeek.FRI, "09:00:00", "10:00:00");

        //  학점초과 강좌(20학점짜리 강좌, 신청 시 초과)
        Course heavy = helper.createCourse("학점초과강좌", major, "OO동 105호",
                30, 0, 20, professorProfile,
                1, 1, "/plans/heavy.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(heavy, DayOfWeek.THU, "13:00:00", "15:00:00");

        //  시간표충돌 강좌
        Course conflict = helper.createCourse("충돌강좌", major, "OO동 106호",
                30, 0, 3, professorProfile,
                1, 1, "/plans/conflict.pdf"
        );
        // 자료구조, 운영체제 수업과 시간표가 겹침
        helper.createCourseScheduleAndAssociateWithCourse(conflict, DayOfWeek.MON, "10:00:00", "11:00:00");
        helper.createCourseScheduleAndAssociateWithCourse(conflict, DayOfWeek.TUE, "10:00:00", "11:00:00");
    }

    /**
     * 시간표 생성 메서드
     */
    private void initTimeTable(Member student) {
        helper.createTimetable(student, 2025, 1);
    }
}