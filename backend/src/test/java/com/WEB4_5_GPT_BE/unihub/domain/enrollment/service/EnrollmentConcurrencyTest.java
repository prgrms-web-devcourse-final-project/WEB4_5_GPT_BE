package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseScheduleRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.EnrollmentPeriodRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.ProfessorSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.StudentSignUpRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.EmailService;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.MemberService;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.MajorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import com.WEB4_5_GPT_BE.unihub.global.config.RedisTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@RedisTestContainerConfig
class EnrollmentConcurrencyTest {

    @Autowired
    EnrollmentService enrollmentService;
    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    CourseRepository courseRepository;
    @Autowired
    UniversityRepository universityRepository;
    @Autowired
    MajorRepository majorRepository;
    @Autowired
    EmailService emailService;
    @Autowired
    MemberService memberService;
    @Autowired
    CourseScheduleRepository courseScheduleRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    EnrollmentPeriodRepository enrollmentPeriodRepository;

    private List<Member> students;
    private Course course;

    @BeforeEach
    void SetUp() {
        University university = universityRepository.save(University.builder().name("B대학교").emailDomain("buni.ac.kr").build());
        Major major = majorRepository.save(Major.builder().name("인공지능전공").university(university).build());

        students = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> {
                    String email = String.format("concurrentStudent%d@buni.ac.kr", i);
                    emailService.markEmailAsVerified(email);
                    memberService.signUpStudent(new StudentSignUpRequest(
                                    email,
                                    "password",
                                    String.format("테스트학생%d", i),
                                    String.format("2502%04d", i),
                                    university.getId(),
                                    major.getId(),
                                    1,
                                    1,
                                    Role.STUDENT
                            )
                    );
                    return memberRepository.findByEmail(email).orElseThrow();
                }).toList();

        emailService.markEmailAsVerified("professor@buni.ac.kr");
        memberService.signUpProfessor(new ProfessorSignUpRequest("professor@buni.ac.kr", "password",
                "김교수", "EMP00000", university.getId(), major.getId(), Role.PROFESSOR));

        Member professor = memberRepository.findByEmail("professor@buni.ac.kr").orElseThrow();
        professor.getProfessorProfile().setApprovalStatus(ApprovalStatus.APPROVED);

        memberRepository.save(professor);


        // 수강신청 기간 설정 (1학년·1학기)
        int year = LocalDate.now().getYear();
        EnrollmentPeriod period = EnrollmentPeriod.builder()
                .university(university)
                .year(year)
                .grade(1)
                .semester(1)
                .startDate(LocalDate.of(year, 5, 1))
                .endDate(LocalDate.of(year, 5, 31))
                .build();
        enrollmentPeriodRepository.save(period);

        course = Course.builder()
                .title("자료구조")
                .major(major)
                .location("OO동 101호")
                .capacity(30)
                .enrolled(0)
                .credit(3)
                .professor(professor.getProfessorProfile())
                .grade(1)
                .semester(1)
                .coursePlanAttachment("/plans/ds.pdf")
                .build();

        courseRepository.save(course);

        CourseSchedule cs = courseScheduleRepository.save(
                CourseSchedule.builder()
                        .course(course)
                        .universityId(university.getId())
                        .location(course.getLocation())
                        .professorProfileEmployeeId(professor.getProfessorProfile().getEmployeeId())
                        .day(DayOfWeek.MON)
                        .startTime(LocalTime.parse("09:00:00"))
                        .endTime(LocalTime.parse("10:30:00"))
                        .build()
        );

        course.getSchedules().add(cs);
        courseRepository.save(course);
    }

    @Test
    @DisplayName("동시 수강신청 시 정원 초과 방지")
    void concurrentEnrollment_test() throws InterruptedException {

        // 2) 100개 쓰레드 띄워서 동시 수강신청
        ExecutorService exec = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(100);
        for (Member s : students) {
            exec.submit(() -> {
                try {
                    enrollmentService.enrollment(s, course.getId());
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        exec.shutdown();

        Course savedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(savedCourse.getEnrolled()).isEqualTo(30);

        // 3) 저장된 신청 건수 검증 (정원만큼만)
        List<Enrollment> enrollments = enrollmentRepository.findAllByCourseId(course.getId());
        assertThat(enrollments.size()).isEqualTo(30);


    }
}
