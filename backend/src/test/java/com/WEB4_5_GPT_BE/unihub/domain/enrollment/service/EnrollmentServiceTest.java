package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.CourseNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.EnrollmentPeriodRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception.*;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("수강신청 관련 단위 테스트")
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
        assertThat(result).hasSize(1);
        MyEnrollmentResponse dto = result.get(0);

        assertThat(dto.enrollmentId()).isEqualTo(1L);
        assertThat(dto.courseId()).isEqualTo(101L);
        assertThat(dto.majorName()).isEqualTo("컴퓨터공학과");
        assertThat(dto.courseTitle()).isEqualTo("자료구조");
        assertThat(dto.professorName()).isEqualTo("김교수");
        assertThat(dto.location()).isEqualTo("OO동 401호");

        assertThat(dto.schedule()).hasSize(2);
        assertThat(dto.schedule().get(0).day()).isEqualTo("MON");
        assertThat(dto.schedule().get(0).startTime()).isEqualTo("09:00");
        assertThat(dto.schedule().get(0).endTime()).isEqualTo("10:30");
        assertThat(dto.schedule().get(1).day()).isEqualTo("FRI");
        assertThat(dto.schedule().get(1).startTime()).isEqualTo("14:00");
        assertThat(dto.schedule().get(1).endTime()).isEqualTo("15:30");

        assertThat(dto.credit()).isEqualTo(3);
        assertThat(dto.grade()).isEqualTo(3);
        assertThat(dto.semester()).isEqualTo(2);
        assertThat(dto.capacity()).isEqualTo(30);
        assertThat(dto.availableSeats()).isEqualTo(3);
    }

    private EnrollmentPeriod createPeriod(int StartDate, int endDate) {
        LocalDate today = LocalDate.now();

        return EnrollmentPeriod.builder()
                .university(profile.getUniversity())
                .year(today.getYear())
                .grade(profile.getGrade())
                .semester(profile.getSemester())
                .startDate(today.minusDays(StartDate))
                .endDate(today.plusDays(endDate))
                .build();
    }

    private void stubEnrollmentPeriod(EnrollmentPeriod period) {
        LocalDate today = LocalDate.now();
        when(enrollmentPeriodRepository
                .findByUniversityIdAndYearAndGradeAndSemester(
                        eq(profile.getUniversity().getId()),
                        eq(today.getYear()),
                        eq(profile.getGrade()),
                        eq(profile.getSemester())
                ))
                .thenReturn(Optional.of(period));
    }

    private Course stubCourse(int capacity, int enrolled, int credit) {

        Major major = Major.builder().id(1L).name("전공").build();

        ProfessorProfile profProfile = ProfessorProfile.builder()
                .member(Member.builder().name("교수님").build())
                .build();

        Course course = Course.builder()
                .id(COURSE_ID)
                .title("테스트강좌")
                .major(major)
                .location("강의실")
                .capacity(capacity)
                .enrolled(enrolled)
                .credit(credit)
                .professor(profProfile)
                .grade(profile.getGrade())
                .semester(profile.getSemester())
                .build();

        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course));

        return course;
    }

    @Test
    @DisplayName("수강 취소 - 성공")
    void cancelMyEnrollment_success() {

        // 1) 수강신청 기간 생성
        EnrollmentPeriod period = createPeriod(1, 1);

        // 학생정보로 수강신청 기간 검색 시 만들어준 신청 기간이 반환되도록 stub
        stubEnrollmentPeriod(period);

        // 2) 기존 수강신청 내역 조회 stub
        Course course = Course.builder()
                .id(COURSE_ID)
                .title("테스트강좌")
                .capacity(30)
                .enrolled(10)  // 취소 전 인원
                .build();
        Enrollment enrollment = Enrollment.builder()
                .id(42L)
                .student(profile)
                .course(course)
                .build();
        when(enrollmentRepository.findByCourseIdAndStudentId(COURSE_ID, profile.getId()))
                .thenReturn(Optional.of(enrollment));

        // 3) 실행 (예외 없이 정상 종료)
        enrollmentService.cancelMyEnrollment(studentMember, COURSE_ID);

        // 4) delete 호출 검증
        verify(enrollmentRepository).delete(enrollment);

        // 5) Course.enrolled가 9로 감소했는지 검증
        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();
        assertThat(saved.getEnrolled()).isEqualTo(9);
    }

    @Test
    @DisplayName("수강 취소 실패 - 수강신청 기간 정보 없음")
    void cancelMyEnrollment_throws1() {
        LocalDate today = LocalDate.now();

        // enrollmentPeriodRepository에서 빈 Optional 반환
        when(enrollmentPeriodRepository
                .findByUniversityIdAndYearAndGradeAndSemester(
                        eq(profile.getUniversity().getId()),
                        eq(today.getYear()),
                        eq(profile.getGrade()),
                        eq(profile.getSemester())
                ))
                .thenReturn(Optional.empty());

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.cancelMyEnrollment(studentMember, COURSE_ID)
        ).isInstanceOf(EnrollmentPeriodNotFoundException.class);

        // enrollmentRepository는 호출되지 않아야 함
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    @DisplayName("수강 취소 실패 - 수강신청 기간 외 요청인 경우")
    void cancelMyEnrollment_throws2() {
        LocalDate today = LocalDate.now();

        // enrollmentPeriodRepository에서 today가 기간 밖인 EnrollmentPeriod 반환
        EnrollmentPeriod period = EnrollmentPeriod.builder()
                .university(profile.getUniversity())
                .year(today.getYear())
                .grade(profile.getGrade())
                .semester(profile.getSemester())
                .startDate(today.plusDays(1))   // 시작일이 내일
                .endDate(today.plusDays(2))     // 종료일이 모레
                .build();

        // 학생정보로 수강신청 기간 검색 시 만들어준 신청 기간이 반환되도록 stub
        stubEnrollmentPeriod(period);

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.cancelMyEnrollment(studentMember, COURSE_ID)
        ).isInstanceOf(EnrollmentPeriodClosedException.class);

        // enrollmentRepository는 호출되지 않아야 함
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    @DisplayName("수강 취소 실패 - 수강신청 내역이 없는 경우")
    void cancelMyEnrollment_throws3() {
        // 1) 수강신청 기간 생성
        EnrollmentPeriod period = createPeriod(1, 1);

        // 학생정보로 수강신청 기간 검색 시 만들어준 신청 기간이 반환되도록 stub
        stubEnrollmentPeriod(period);

        // 수강신청 내역이 없도록 stub
        when(enrollmentRepository.findByCourseIdAndStudentId(COURSE_ID, profile.getId()))
                .thenReturn(Optional.empty());

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.cancelMyEnrollment(studentMember, COURSE_ID))
                .isInstanceOf(EnrollmentNotFoundException.class);

        // delete가 호출되지 않아야 함
        verify(enrollmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("수강 신청 - 성공")
    void enrollment_success() {

        // 1) 수강신청 기간 생성
        EnrollmentPeriod period = createPeriod(1, 1);

        // 학생정보로 수강신청 기간 검색 시 만들어준 신청 기간이 반환되도록 stub
        stubEnrollmentPeriod(period);

        // 2) 강좌 조회 stub (최대인원 10, 현재수강인원 5, 학점 3)
        Course course = stubCourse(10, 5, 3);

        // 3) 중복 신청 없음
        when(enrollmentRepository.existsByCourseIdAndStudentId(COURSE_ID, profile.getId()))
                .thenReturn(false);

        // 4) 기존 신청 내역 없음 (학점·스케줄 통과)
        when(enrollmentRepository.findAllByStudent(profile))
                .thenReturn(Collections.emptyList());

        // 5) 실행 (예외 없이 정상)
        assertThatCode(() ->
                enrollmentService.enrollment(studentMember, COURSE_ID)
        ).doesNotThrowAnyException();

        // 6) save 호출 검증
        ArgumentCaptor<Enrollment> enrollCaptor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(enrollCaptor.capture());

        Enrollment savedEnroll = enrollCaptor.getValue();
        assertThat(savedEnroll.getStudent()).isEqualTo(profile);
        assertThat(savedEnroll.getCourse()).isEqualTo(course);

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(courseCaptor.capture());
        Course savedCourse = courseCaptor.getValue();
        assertThat(savedCourse.getEnrolled()).isEqualTo(6);
    }

    @Test
    @DisplayName("수강 신청 실패 – 강좌 정보가 없는 경우")
    void enrollment_throws1() {
        LocalDate today = LocalDate.now();

        // 1) 수강신청 기간 생성
        EnrollmentPeriod period = createPeriod(1, 1);

        // 학생정보로 수강신청 기간 검색 시 만들어준 신청 기간이 반환되도록 stub
        stubEnrollmentPeriod(period);

        // 2) 강좌 정보 없음 stub
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.empty());

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(studentMember, COURSE_ID)
        ).isInstanceOf(CourseNotFoundException.class);

        // enrollmentRepository는 호출되지 않아야 함
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    @DisplayName("수강 신청 실패 – 수강신청 기간 정보가 없는 경우")
    void enrollment_throws2() {
        LocalDate today = LocalDate.now();

        // enrollmentPeriodRepository에서 빈 Optional 반환
        when(enrollmentPeriodRepository
                .findByUniversityIdAndYearAndGradeAndSemester(
                        eq(profile.getUniversity().getId()),
                        eq(today.getYear()),
                        eq(profile.getGrade()),
                        eq(profile.getSemester())
                ))
                .thenReturn(Optional.empty());

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(studentMember, COURSE_ID)
        ).isInstanceOf(EnrollmentPeriodNotFoundException.class);

        // enrollmentRepository는 호출되지 않아야 함
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    @DisplayName("수강 신청 실패 – 수강신청 기간 외 요청인 경우")
    void enrollment_throws3() {

        LocalDate today = LocalDate.now();

        // enrollmentPeriodRepository에서 today가 기간 밖인 EnrollmentPeriod 반환
        EnrollmentPeriod period = EnrollmentPeriod.builder()
                .university(profile.getUniversity())
                .year(today.getYear())
                .grade(profile.getGrade())
                .semester(profile.getSemester())
                .startDate(today.plusDays(1))   // 시작일이 내일
                .endDate(today.plusDays(2))     // 종료일이 모레
                .build();

        // 학생정보로 수강신청 기간 검색 시 만들어준 신청 기간이 반환되도록 stub
        stubEnrollmentPeriod(period);

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(studentMember, COURSE_ID)
        ).isInstanceOf(EnrollmentPeriodClosedException.class);

        verifyNoInteractions(enrollmentRepository);
        verifyNoInteractions(courseRepository);
    }

    @Test
    @DisplayName("수강 신청 실패 – 정원 초과 시")
    void enrollment_throws4() {
        LocalDate today = LocalDate.now();

        // 1) 수강신청 기간 생성
        EnrollmentPeriod period = createPeriod(1, 1);

        // 학생정보로 수강신청 기간 검색 시 만들어준 신청 기간이 반환되도록 stub
        stubEnrollmentPeriod(period);

        // 2) 강좌 조회 stub (최대인원 10, 현재수강인원 10, 학점 3) 정원이 다 찬 상태
        stubCourse(10, 10, 3);

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(studentMember, COURSE_ID)
        ).isInstanceOf(CourseCapacityExceededException.class);

        // 저장 동작이 일어나지 않아야 함
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 실패 – 동일 강좌 중복 신청 시")
    void enrollment_throws5() {
        LocalDate today = LocalDate.now();

        // 1) 수강신청 기간 생성
        EnrollmentPeriod period = createPeriod(1, 1);

        // 학생정보로 수강신청 기간 검색 시 만들어준 신청 기간이 반환되도록 stub
        stubEnrollmentPeriod(period);

        // 2) 강좌 조회 stub (최대인원 10, 현재수강인원 5, 학점 3)
        stubCourse(10, 5, 3);

        // 3) 이미 신청한 강좌로 stub
        when(enrollmentRepository.existsByCourseIdAndStudentId(COURSE_ID, profile.getId()))
                .thenReturn(true);

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(studentMember, COURSE_ID)
        ).isInstanceOf(DuplicateEnrollmentException.class);

        // save 호출되지 않아야 함
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 실패 – 최대 학점 초과 시")
    void enrollment_throws6() {
        LocalDate today = LocalDate.now();

        // 1) 수강신청 기간 생성
        EnrollmentPeriod period = createPeriod(1, 1);

        // 학생정보로 수강신청 기간 검색 시 만들어준 신청 기간이 반환되도록 stub
        stubEnrollmentPeriod(period);

        // 2) 강좌 조회 stub (최대인원 10, 현재수강인원 5, 학점 3)
        stubCourse(10, 5, 3);

        // 3) 중복 신청 없음 stub
        when(enrollmentRepository.existsByCourseIdAndStudentId(COURSE_ID, profile.getId()))
                .thenReturn(false);

        // 4) 기존 신청 학점이 19점인 강좌 1개 → 총 19 + 새 강좌 3 = 22 > 21
        Course existingCourse = Course.builder()
                .id(200L)
                .credit(19)
                .build();
        Enrollment existingEnrollment = Enrollment.builder()
                .id(2L)
                .student(profile)
                .course(existingCourse)
                .build();
        when(enrollmentRepository.findAllByStudent(profile))
                .thenReturn(List.of(existingEnrollment));

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(studentMember, COURSE_ID)
        ).isInstanceOf(CreditLimitExceededException.class);

        // save 호출이 일어나지 않아야 함
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 실패 – 시간표 충돌 시")
    void enrollment_throws7() {

        // 1) 수강신청 기간 생성
        EnrollmentPeriod period = createPeriod(1, 1);

        // 학생정보로 수강신청 기간 검색 시 만들어준 신청 기간이 반환되도록 stub
        stubEnrollmentPeriod(period);

        // 2) 신규 강좌 stub (MON 10:00–11:00)
        Major major = Major.builder().id(1L).name("전공").build();
        ProfessorProfile profProfile = ProfessorProfile.builder()
                .member(Member.builder().name("교수님").build())
                .build();
        Course newCourse = Course.builder()
                .id(COURSE_ID)
                .title("신규강좌")
                .major(major)
                .location("강의실")
                .capacity(10)
                .enrolled(5)
                .credit(3)
                .professor(profProfile)
                .grade(profile.getGrade())
                .semester(profile.getSemester())
                .build();
        CourseSchedule newSchedule = CourseSchedule.builder()
                .day(DayOfWeek.MON)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build();
        newSchedule.setCourse(newCourse);
        newCourse.getSchedules().add(newSchedule);
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(newCourse));

        // 3) 중복 신청 없음 stub
        when(enrollmentRepository.existsByCourseIdAndStudentId(COURSE_ID, profile.getId()))
                .thenReturn(false);

        // 4) 기존 신청 내역에 시간표 충돌하는 강좌(MON 10:30–12:00) 추가
        Course existingCourse = Course.builder()
                .id(999L)
                .credit(3)
                .build();
        CourseSchedule existSchedule = CourseSchedule.builder()
                .day(DayOfWeek.MON)
                .startTime(LocalTime.of(10, 30))
                .endTime(LocalTime.of(12, 0))
                .build();
        existSchedule.setCourse(existingCourse);
        existingCourse.getSchedules().add(existSchedule);
        Enrollment existingEnrollment = Enrollment.builder()
                .student(profile)
                .course(existingCourse)
                .build();
        when(enrollmentRepository.findAllByStudent(profile))
                .thenReturn(List.of(existingEnrollment));

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(studentMember, COURSE_ID)
        ).isInstanceOf(ScheduleConflictException.class);

        // save 호출이 일어나지 않아야 함
        verify(enrollmentRepository, never()).save(any());
    }
}