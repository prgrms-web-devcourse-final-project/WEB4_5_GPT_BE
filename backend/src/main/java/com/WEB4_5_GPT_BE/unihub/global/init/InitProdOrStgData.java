package com.WEB4_5_GPT_BE.unihub.global.init;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import com.WEB4_5_GPT_BE.unihub.global.infra.s3.S3Service;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@Profile({"stg", "prod"}) // 테스트, 운영 서버 환경에서만 동작하는 테스트 데이터 초기화
@RequiredArgsConstructor
public class InitProdOrStgData {

    private final InitDataHelper helper;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;

    @PostConstruct
    @Transactional
    public void init() {
        // 이미 데이터가 있으면 교수 승인 상태만 보정하고 종료
        if (helper.countMembers() > 0) {
            helper.ensureProfessorApprovalStatus("professor1@auni.ac.kr", ApprovalStatus.APPROVED);
            return;
        }

        // 1) 대학·전공 생성
        University university = helper.createUniversity("A대학교", "auni.ac.kr");
        Major majorSW = helper.createMajor("소프트웨어전공", university);
        Major majorCS = helper.createMajor("컴퓨터공학전공", university);

        // 2) 1학년 학생·교수·관리자 계정 생성
        helper.createStudent(
                "haneulkim@auni.ac.kr", "studentPw", "김하늘", "20250001",
                university.getId(), majorSW.getId(), 1, 1
        );
        Member professor1 = helper.createProfessor(
                "professor1@auni.ac.kr", "password", "김교수", "EMP20250001",
                university.getId(), majorSW.getId(), ApprovalStatus.APPROVED
        );
        Member professor2 = helper.createProfessor(
                "professor2@auni.ac.kr", "password", "이교수", "EMP20250002",
                university.getId(), majorCS.getId(), ApprovalStatus.APPROVED
        );
        helper.createAdmin("adminmaster@auni.ac.kr", "adminPw", "최고관리자", passwordEncoder);

        // 3) 1학년 1학기 수강신청 기간 생성 (/api/admin/enrollment-periods)
        helper.createEnrollmentPeriod(
                university,
                LocalDate.now().getYear(), 1, 1,
                LocalDate.of(2025, 5, 1),
                LocalDate.of(2025, 5, 31)
        );

        // 4) SW전공 테스트 강좌 4개 + 스케줄 생성 (/api/courses)
        List<Course> swCourses = initCoursesForSw((Professor) professor1, majorSW);

        // 5) 김하늘 학생이 두 강좌 수강신청 (/api/enrollments)
        Member student1 = helper.getMemberByEmail("haneulkim@auni.ac.kr");
        helper.createEnrollment(student1, swCourses.get(0).getId());
        helper.createEnrollment(student1, swCourses.get(1).getId());

        // 6) 2학년 학생 생성 (/api/members/signup/student)
        helper.createStudent(
                "secondstudent@auni.ac.kr", "studentPw2", "박학생", "20250002",
                university.getId(), majorSW.getId(), 2, 1
        );
        Member student2 = helper.getMemberByEmail("secondstudent@auni.ac.kr");

        // 7) 2학년 1학기 수강신청 기간 생성
        helper.createEnrollmentPeriod(
                university,
                LocalDate.now().getYear(), 2, 1,
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 6, 30)
        );

        // 8) 컴공전공 강좌 3개 + 스케줄 생성
        List<Course> csCourses = initCoursesForCS((Professor) professor2, majorCS);

        // 9) 2학년 학생이 첫 번째 컴공전공 강좌 수강신청 (기간 검증용)
        helper.createEnrollment(student2, csCourses.get(0).getId());

        // 10) 승인 대기 중인 교수 한 명 생성
        helper.createProfessor(
                "pendingprof@auni.ac.kr", "password", "박교수", "EMP20250003",
                university.getId(), majorCS.getId(), ApprovalStatus.PENDING
        );

        initConflictCourses((Professor) professor2, majorSW);

        //공지사항 생성
        initNotices();

        // 11) 2학년 학생 1명 더 생성
        helper.createStudent(
                "thirdstudent@auni.ac.kr", "studentPw", "홍길동", "20250003",
                university.getId(), majorSW.getId(), 2, 1
        );

        // 12) 1학년 학생 1명 더 생성
        helper.createStudent(
                "fourthstudent@auni.ac.kr", "studentPw", "정학생", "20231423",
                university.getId(), majorSW.getId(), 1, 1
        );

        // 13) 4학년 1학기 수강신청 기간 생성
        helper.createEnrollmentPeriod(
                university,
                LocalDate.now().getYear(), 4, 1,
                LocalDate.of(2025, 5, 1),
                LocalDate.of(2025, 5, 30)
        );

        // 14) 동시성 테스트용 데이터 생성 (4학년 학생 100명, 교수 1명, 강좌 1개)
        createTestDataForConcurrency(university, majorSW);
    }

    /**
     * 소프트웨어 공학전공 강좌 생성 메서드
     */
    private List<Course> initCoursesForSw(Professor professor, Major major) {

        // 자료구조
        Course dataStructure = helper.createCourse(
                "자료구조", major, "교육관 401호",
                30, 0, 3,
                professor,
                3, 2, "/plans/data-structure.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(dataStructure, DayOfWeek.MON, "09:00:00", "10:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(dataStructure, DayOfWeek.FRI, "14:00:00", "15:30:00");

        // 운영체제
        Course os = helper.createCourse(
                "운영체제", major, "교육관 402호",
                30, 0, 2,
                professor,
                3, 2, "/plans/os.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(os, DayOfWeek.TUE, "09:00:00", "10:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(os, DayOfWeek.THU, "14:00:00", "15:30:00");

        // 네트워크
        Course network = helper.createCourse(
                "네트워크", major, "교육관 403호",
                25, 0, 3,
                professor,
                3, 2, "/plans/network.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(network, DayOfWeek.WED, "10:00:00", "11:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(network, DayOfWeek.FRI, "16:00:00", "17:30:00");

        Course javaCourse = helper.createCourse(
                "자바 프로그래밍", major, "교육관 301호",
                40, 0, 3,
                professor,
                1, 1, "/plans/java.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(javaCourse, DayOfWeek.MON, "09:00", "11:00");

        return List.of(dataStructure, os, network, javaCourse);
    }

    /**
     * 컴퓨터공학전공 강좌 생성 메서드
     */
    private List<Course> initCoursesForCS(Professor professor, Major major) {
        Course algo = helper.createCourse(
                "알고리즘", major, "공학관 201호",
                30, 0, 3, professor,
                2, 1, "/plans/algorithm.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(algo, DayOfWeek.MON, "10:00:00", "11:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(algo, DayOfWeek.WED, "13:00:00", "14:30:00");

        Course db = helper.createCourse(
                "데이터베이스", major, "공학관 202호",
                35, 0, 3, professor,
                2, 1, "/plans/database.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(db, DayOfWeek.TUE, "09:00:00", "10:30:00");
        helper.createCourseScheduleAndAssociateWithCourse(db, DayOfWeek.THU, "14:00:00", "15:30:00");

        Course ai = helper.createCourse(
                "인공지능", major, "공학관 203호",
                25, 0, 3, professor,
                2, 1, "/plans/ai.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(ai, DayOfWeek.FRI, "11:00:00", "12:30:00");

        return List.of(algo, db, ai);
    }

    private void initConflictCourses(Professor professor, Major major) {
        // 1) 정원초과 강좌 (capacity=30, 이미 enrolled=30 신청함)
        Course full = helper.createCourse(
                "정원초과강좌", major, "OO동 104호",
                30, 30,
                3,                          // credit
                professor,
                1, 1, "/plans/full.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(
                full, DayOfWeek.FRI, "09:00:00", "10:00:00"
        );

        // 2) 학점초과 강좌 (21학점짜리, 신청 시 반드시 초과)
        Course heavy = helper.createCourse(
                "학점초과강좌", major, "OO동 105호",
                30, 0,                      // capacity=30, enrolled=0
                21,                         // credit=20
                professor,
                1, 1, "/plans/heavy.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(
                heavy, DayOfWeek.THU, "13:00:00", "15:00:00"
        );
        // 3) 시간표충돌 강좌 (모든 요일, 하루종일 스케줄)
        Course conflict = helper.createCourse(
                "충돌강좌", major, "OO동 106호",
                30, 0, 3,
                professor,
                1, 1, "/plans/conflict.pdf"
        );
        // 모든 DayOfWeek 에 00:00~23:59 스케줄 추가
        for (DayOfWeek day : DayOfWeek.values()) {
            helper.createCourseScheduleAndAssociateWithCourse(
                    conflict, day, "00:00:00", "23:59:59"
            );
        }
    }
    private void initNotices() {
        helper.createNotice("필독 공지", "수강신청 일정 공지", null);
        helper.createNotice("시스템 점검 안내",
                "안녕하세요.\n더 나은 서비스를 제공하기 위해 시스템 점검이 예정되어 있습니다.",
                null
        );

        String summerUrl;
        try {
            // classpath의 static/notice-summer.jpg 파일을 읽어 MultipartFile로 변환
            ClassPathResource resource = new ClassPathResource("static/notice-summer.jpg");
            MultipartFile file = new MockMultipartFile(
                    "file",
                    resource.getFilename(),
                    MediaType.IMAGE_JPEG_VALUE,
                    resource.getInputStream()
            );
            // S3에 업로드하고 URL 받기
            summerUrl = s3Service.upload(file);
        } catch (IOException e) {
            log.error("테스트용 공지사항 이미지 업로드 중 오류 발생", e);
            throw new UnihubException("500", "테스트 데이터용 공지사항 이미지 업로드 실패");
        }
        log.info("테스트용 공지 이미지 업로드 URL = {}", summerUrl);
        // 업로드한 URL을 사용해 공지사항 생성
        helper.createNotice("여름학기 수강신청 안내",
                "여름학기 수강신청이 시작됩니다.",
                summerUrl
        );
    }

    private void createTestDataForConcurrency(University university, Major majorSW) {
        for (int i = 1; i <= 100; i++) {
            helper.createStudent(
                    "concurrencyStudent" + i + "@auni.ac.kr", "studentPw", "동시성학생" + i, "2504%04d".formatted(i),
                    university.getId(), majorSW.getId(), 4, 1
            );
        }

        Professor professor = (Professor) helper.createProfessor(
                "concurrencyProfessor@auni.ac.kr", "password", "동시성교수", "EMP250011",
                university.getId(), majorSW.getId(), ApprovalStatus.APPROVED
        );

        // 자료구조
        Course dataStructure = helper.createCourse(
                "동시성과목", majorSW, "동시관 401호",
                30, 0, 3,
                professor,
                4, 1, "/plans/data-structure.pdf"
        );
        helper.createCourseScheduleAndAssociateWithCourse(dataStructure, DayOfWeek.MON, "09:00:00", "10:30:00");

    }
}