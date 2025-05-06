package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.EnrollmentPeriodRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EnrollmentPeriodRepository enrollmentPeriodRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Member studentMember;
    private StudentProfile profile;
    private final Long COURSE_ID = 100L;

    @BeforeEach
    void setUp() {
        // Member ↔ StudentProfile ↔ University 연결
        studentMember = Member.builder()
                .email("stud@uni.ac.kr")
                .password("pw")
                .name("테스트학생")
                .build();

        profile = StudentProfile.builder()
                .id(1L)
                .member(studentMember)
                .studentCode("S20250001")
                .grade(2)
                .semester(1)
                .build();

        // 학생 소속 대학 세팅
        University uni = University.builder()
                .id(10L)
                .name("테스트대학")
                .build();
        profile.setUniversity(uni);

        studentMember.setStudentProfile(profile);
    }

    @Test
    @DisplayName("내 수강목록 조회 - 성공")
    void getMyEnrollmentList_success() {
        // --- 준비: Course, ProfessorProfile, Schedule, Enrollment 셋업 ---
        Major major = Major.builder().name("컴퓨터공학과").build();

        Member profMember = Member.builder()
                .email("prof@uni.ac.kr")
                .password("pw")
                .name("김교수")
                .role(null)
                .build();
        ProfessorProfile profProfile = ProfessorProfile.builder()
                .member(profMember)
                .build();

        Course course = Course.builder()
                .id(101L)
                .title("자료구조")
                .major(major)
                .location("OO동 401호")
                .capacity(30)
                .enrolled(27)    // availableSeats = 3
                .credit(3)
                .professor(profProfile)
                .grade(3)
                .semester(2)
                .build();

        CourseSchedule cs1 = CourseSchedule.builder()
                .day(DayOfWeek.MON)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .build();
        CourseSchedule cs2 = CourseSchedule.builder()
                .day(DayOfWeek.FRI)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 30))
                .build();
        cs1.setCourse(course);
        cs2.setCourse(course);
        course.getSchedules().addAll(List.of(cs1, cs2));

        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .student(profile)
                .course(course)
                .build();

        // --- findAllByStudent 반환값 지정 ---
        when(enrollmentRepository.findAllByStudent(profile))
                .thenReturn(List.of(enrollment));

        // --- 실행 ---
        List<MyEnrollmentResponse> result = enrollmentService.getMyEnrollmentList(studentMember);

        // --- 검증 ---
        assertEquals(1, result.size());
        MyEnrollmentResponse dto = result.get(0);

        assertEquals(1L, dto.enrollmentId());
        assertEquals(101L, dto.courseId());
        assertEquals("컴퓨터공학과", dto.majorName());
        assertEquals("자료구조", dto.courseTitle());
        assertEquals("김교수", dto.professorName());
        assertEquals("OO동 401호", dto.location());

        assertEquals(2, dto.schedule().size());
        assertEquals("MON", dto.schedule().get(0).day());
        assertEquals("09:00", dto.schedule().get(0).startTime());
        assertEquals("10:30", dto.schedule().get(0).endTime());
        assertEquals("FRI", dto.schedule().get(1).day());
        assertEquals("14:00", dto.schedule().get(1).startTime());
        assertEquals("15:30", dto.schedule().get(1).endTime());

        assertEquals(3, dto.credit());
        assertEquals(3, dto.grade());
        assertEquals(2, dto.semester());
        assertEquals(30, dto.capacity());
        assertEquals(3, dto.availableSeats());
    }

    @Test
    @DisplayName("수강 취소 - 성공")
    void cancelMyEnrollment_success() {
        LocalDate today = LocalDate.now();

        // 1) 수강신청 기간 조회
        EnrollmentPeriod period = EnrollmentPeriod.builder()
                .id(5L)
                .university(profile.getUniversity())
                .year(today.getYear())
                .grade(profile.getGrade())
                .semester(profile.getSemester())
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(1))
                .build();

        when(enrollmentPeriodRepository
                .findByUniversityIdAndYearAndGradeAndSemester(
                        eq(profile.getUniversity().getId()),
                        eq(today.getYear()),
                        eq(profile.getGrade()),
                        eq(profile.getSemester())
                ))
                .thenReturn(Optional.of(period));

        // 2) 기존 수강신청 내역 조회 stub
        Enrollment enrollment = Enrollment.builder()
                .id(42L)
                .student(profile)
                .build();
        when(enrollmentRepository.findByCourseIdAndStudentId(COURSE_ID, profile.getId()))
                .thenReturn(Optional.of(enrollment));

        // 3) 실행 (예외 없이 정상 종료)
        enrollmentService.cancelMyEnrollment(studentMember, COURSE_ID);

        // 4) delete 호출 검증
        verify(enrollmentRepository).delete(enrollment);
    }


    @Test
    @DisplayName("수강 신청 - 성공")
    void enrollment_success() {
        LocalDate today = LocalDate.now();

        // 1) 수강신청 기간 stub
        EnrollmentPeriod period = EnrollmentPeriod.builder()
                .university(profile.getUniversity())
                .year(today.getYear())
                .grade(profile.getGrade())
                .semester(profile.getSemester())
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(1))
                .build();
        when(enrollmentPeriodRepository
                .findByUniversityIdAndYearAndGradeAndSemester(
                        eq(profile.getUniversity().getId()),
                        eq(today.getYear()),
                        eq(profile.getGrade()),
                        eq(profile.getSemester())
                ))
                .thenReturn(Optional.of(period));

        // 2) 강좌 조회 stub
        Major major = Major.builder().id(5L).name("전공").build();
        Member profMem = Member.builder()
                .email("prof@uni.ac.kr")
                .password("pw")
                .name("교수님")
                .build();
        ProfessorProfile profProfile = ProfessorProfile.builder()
                .member(profMem)
                .build();

        Course course = Course.builder()
                .id(COURSE_ID)
                .title("테스트강좌")
                .major(major)
                .location("강의실")
                .capacity(10)
                .enrolled(5)
                .credit(3)
                .professor(profProfile)
                .grade(profile.getGrade())
                .semester(profile.getSemester())
                .build();
        // no existing schedules => no conflict
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course));

        // 3) 중복 신청 없음
        when(enrollmentRepository.existsByCourseIdAndStudentId(COURSE_ID, profile.getId()))
                .thenReturn(false);

        // 4) 기존 신청 내역 없음 (학점·스케줄 통과)
        when(enrollmentRepository.findAllByStudent(profile))
                .thenReturn(Collections.emptyList());

        // 5) 실행 (예외 없이 정상)
        assertDoesNotThrow(() ->
                enrollmentService.enrollment(studentMember, COURSE_ID)
        );

        // 6) save 호출 검증
        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());

        Enrollment saved = captor.getValue();
        assertEquals(profile, saved.getStudent());
        assertEquals(course, saved.getCourse());
    }
}
