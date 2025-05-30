package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.CourseNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.EnrollmentPeriodRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response.StudentEnrollmentPeriodResponse;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception.*;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.cancel.EnrollmentCancelCommand;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.async.enroll.EnrollmentCommand;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.time.LocalTime;
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

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private RedissonClient redisson;

    @Mock
    private RBlockingQueue<EnrollmentCommand> enrollQueue;

    @Mock
    private RBlockingQueue<EnrollmentCancelCommand> cancelQueue;

    @Mock
    private EnrollmentValidator enrollmentValidator;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Student profile;
    private final Long COURSE_ID = 100L;

    @BeforeEach
    void setUp() {
        // 학생 소속 대학
        University uni = University.builder()
                .id(10L)
                .name("테스트대학")
                .build();
        // Member ↔ StudentProfile ↔ University 연결
        profile = Student.builder()
                .id(1L)
                .email("stud@uni.ac.kr")
                .password("pw")
                .name("테스트학생")
                .studentCode("S20250001")
                .university(uni)
                .grade(2)
                .semester(1)
                .build();

        lenient().when(studentRepository.findById(profile.getId()))
                .thenReturn(Optional.ofNullable(profile));
    }

    @Test
    @DisplayName("내 수강목록 조회 - 성공")
    void getMyEnrollmentList_success() {
        // --- 준비: Course, ProfessorProfile, Schedule, Enrollment 셋업 ---
        Major major = Major.builder().name("컴퓨터공학과").build();

        Professor profMember = Professor.builder()
                .email("prof@uni.ac.kr")
                .password("pw")
                .name("김교수")
                .build();

        Course course = Course.builder()
                .id(101L)
                .title("자료구조")
                .major(major)
                .location("OO동 401호")
                .capacity(30)
                .enrolled(27)    // availableSeats = 3
                .credit(3)
                .professor(profMember)
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
        List<MyEnrollmentResponse> result = enrollmentService.getMyEnrollmentList(profile);

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

    private EnrollmentPeriod createNotYetPeriod(int StartDate, int endDate) {
        LocalDate today = LocalDate.now();

        return EnrollmentPeriod.builder()
                .university(profile.getUniversity())
                .year(today.getYear())
                .grade(profile.getGrade())
                .semester(profile.getSemester())
                .startDate(today.plusDays(StartDate))   // 시작일이 내일
                .endDate(today.plusDays(endDate))     // 종료일이 모레
                .build();
    }

    private void stubEnrollmentPeriod(EnrollmentPeriod period) {
        lenient().when(enrollmentPeriodRepository.findByUniversityIdAndYearAndGradeAndSemester(
                anyLong(),
                anyInt(),
                anyInt(),
                anyInt()
        )).thenReturn(Optional.of(period));
    }

    private Course stubCourse(int capacity, int enrolled, int credit) {

        Major major = Major.builder().id(1L).name("전공").build();

        Professor profProfile = Professor.builder()
                .name("교수님")
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

        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(course));

        return course;
    }

    @Test
    @DisplayName("수강 취소 - 성공")
    void cancelMyEnrollment_success() {

        // 0) Redis AtomicLong Stub
        RAtomicLong counter = mock(RAtomicLong.class);
        when(redisson.getAtomicLong("course:" + COURSE_ID + ":enrolled"))
                .thenReturn(counter);
        when(counter.decrementAndGet()).thenReturn(9L);

        // 3) 수강신청 기간 stub
        EnrollmentPeriod period = createPeriod(1, 1);
        stubEnrollmentPeriod(period);

        // 4) 기존 수강신청 내역 조회 stub
        when(enrollmentRepository.existsByCourseIdAndStudentId(COURSE_ID, profile.getId()))
                .thenReturn(true);

        // --- when ---
        enrollmentService.cancelMyEnrollment(profile.getId(), COURSE_ID);

        // --- 그리고 핵심 호출 검증 ---
        verify(counter, times(1)).decrementAndGet();
    }


    @Test
    @DisplayName("수강 취소 실패 - 수강신청 기간 정보 없음")
    void cancelMyEnrollment_throws1() {

        doThrow(new EnrollmentPeriodNotFoundException())
                .when(enrollmentValidator)
                .ensureEnrollmentPeriodActive(any(Student.class));

        // 2) 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.cancelMyEnrollment(profile.getId(), COURSE_ID)
        ).isInstanceOf(EnrollmentPeriodNotFoundException.class);

        // 3) 삭제(delete)는 절대 호출되면 안 됩니다
        verify(enrollmentRepository, never()).delete(any());
    }


    @Test
    @DisplayName("수강 취소 실패 - 수강신청 기간 외 요청인 경우")
    void cancelMyEnrollment_throws2() {

        doThrow(new EnrollmentPeriodClosedException())
                .when(enrollmentValidator)
                .ensureEnrollmentPeriodActive(any(Student.class));

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.cancelMyEnrollment(profile.getId(), COURSE_ID)
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
        when(enrollmentRepository.existsByCourseIdAndStudentId(COURSE_ID, profile.getId()))
                .thenReturn(false);

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.cancelMyEnrollment(profile.getId(), COURSE_ID))
                .isInstanceOf(EnrollmentNotFoundException.class);

        // delete가 호출되지 않아야 함
        verify(enrollmentRepository, never()).delete(any());
    }

    private void stubRedisCounters(long capacity, long initialEnrolled) {
        RAtomicLong capacityCounter = mock(RAtomicLong.class);
        RAtomicLong enrolledCounter = mock(RAtomicLong.class);

        // 이들의 stub이 테스트 코드 전반에서 모두 lenient 하게 허용되도록
        lenient().when(redisson.getAtomicLong("course:" + COURSE_ID + ":capacity"))
                .thenReturn(capacityCounter);
        lenient().when(redisson.getAtomicLong("course:" + COURSE_ID + ":enrolled"))
                .thenReturn(enrolledCounter);

        // 사전 용량 조회
        lenient().when(capacityCounter.get()).thenReturn(capacity);

        // 자리 확보 경로에서만 쓰이는 증감/큐 추가도 lenient
        lenient().when(enrolledCounter.incrementAndGet()).thenReturn(initialEnrolled + 1);
        lenient().when(enrollQueue.add(any(EnrollmentCommand.class))).thenReturn(true);
    }

    @Test
    @DisplayName("수강 신청 - 성공")
    void enrollment_success() {
        // --- 0) Redis AtomicLong Stub ---
        stubRedisCounters(10L, 5L);

        RAtomicLong counter = mock(RAtomicLong.class);
        when(redisson.getAtomicLong("course:" + COURSE_ID + ":enrolled"))
                .thenReturn(counter);
        when(counter.incrementAndGet()).thenReturn(1L);

        // --- 1) 수강신청 기간 stub ---
        EnrollmentPeriod period = createPeriod(1, 1);
        stubEnrollmentPeriod(period);

        // --- 2) 기타 검증용 stub ---
        stubCourse(10, 5, 3);

        // --- 3) 실행 (예외 없이 정상) ---
        assertThatCode(() ->
                enrollmentService.enrollment(profile.getId(), COURSE_ID)
        ).doesNotThrowAnyException();

        // --- 4) DB 저장 호출은 없어야 한다. ---
        verify(enrollmentRepository, never()).save(any());
        verify(courseRepository, never()).save(any());

        // --- 그리고 핵심 호출 검증 ---
        verify(counter, times(1)).incrementAndGet();
    }

    @Test
    @DisplayName("수강 신청 실패 – 강좌 정보가 없는 경우")
    void enrollment_throws1() {

        // 2) 강좌 정보 없음 stub
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.empty());

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(profile.getId(), COURSE_ID)
        ).isInstanceOf(CourseNotFoundException.class);

        // enrollmentRepository는 호출되지 않아야 함
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    @DisplayName("수강 신청 실패 – 수강신청 기간 정보가 없는 경우")
    void enrollment_throws2() {

        stubRedisCounters(10L, 5L);

        stubCourse(10, 5, 3);

        doThrow(new EnrollmentPeriodNotFoundException())
                .when(enrollmentValidator)
                .ensureEnrollmentAllowed(profile.getId(), COURSE_ID);

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(profile.getId(), COURSE_ID)
        ).isInstanceOf(EnrollmentPeriodNotFoundException.class);

        // enrollmentRepository는 호출되지 않아야 함
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    @DisplayName("수강 신청 실패 – 수강신청 기간 외 요청인 경우")
    void enrollment_throws3() {
        // 1) Redis 카운터, 코스 정보 stub
        stubRedisCounters(10L, 5L);
        stubCourse(10, 5, 3);

        // 2) 수강신청 기간 조회는 닫힘(Closed) 상태 반환
        EnrollmentPeriod period = createNotYetPeriod(1, 2);
        stubEnrollmentPeriod(period);

        // 3) 실제 검증 부분만 예외를 던지도록 설정
        doThrow(new EnrollmentPeriodClosedException())
                .when(enrollmentValidator)
                .ensureEnrollmentAllowed(profile.getId(), COURSE_ID);

        // 4) 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(profile.getId(), COURSE_ID)
        ).isInstanceOf(EnrollmentPeriodClosedException.class);

        // 5) 아무 저장(save) 호출이 없어야 함
        verify(courseRepository, never()).save(any());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 실패 – 정원 초과 시")
    void enrollment_throws4() {
        // --- 0) Redis AtomicLong Stub 준비 ---
        // capacity=10, enrolled=10(만원)
        stubRedisCounters(10L, 10L);

        RBucket<Boolean> mockBucket = mock(RBucket.class);
        doReturn(mockBucket)
                .when(redisson)
                .getBucket(anyString());
        when(mockBucket.delete()).thenReturn(true);

        // --- 1) 수강신청 기간 생성 & stub ---
        EnrollmentPeriod period = createPeriod(1, 1);
        stubEnrollmentPeriod(period);

        // --- 2) 강좌 조회 stub (capacity=10, enrolled=10, credit=3) ---
        stubCourse(10, 10, 3);

        // --- 3) 실행 및 예외 검증 ---
        assertThatThrownBy(() ->
                enrollmentService.enrollment(profile.getId(), COURSE_ID)
        ).isInstanceOf(CourseCapacityExceededException.class);

        // --- 4) 어떤 저장도 일어나면 안 됩니다 ---
        verify(enrollmentRepository, never()).save(any());
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 실패 – 동일 강좌 중복 신청 시")
    void enrollment_throws5() {

        stubRedisCounters(10L, 5L);

        // 1) 수강신청 기간 생성
        EnrollmentPeriod period = createPeriod(1, 1);

        // 학생정보로 수강신청 기간 검색 시 만들어준 신청 기간이 반환되도록 stub
        stubEnrollmentPeriod(period);

        // 2) 강좌 조회 stub (최대인원 10, 현재수강인원 5, 학점 3)
        stubCourse(10, 5, 3);

        doThrow(new DuplicateEnrollmentException())
                .when(enrollmentValidator)
                .ensureEnrollmentAllowed(profile.getId(), COURSE_ID);

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(profile.getId(), COURSE_ID)
        ).isInstanceOf(DuplicateEnrollmentException.class);

        // save 호출되지 않아야 함
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 실패 – 최대 학점 초과 시")
    void enrollment_throws6() {
        // 0) Redis 카운터 stub
        stubRedisCounters(10L, 5L);

        // 1) 수강신청 기간 stub
        EnrollmentPeriod period = createPeriod(1, 1);
        stubEnrollmentPeriod(period);

        // 2) 강좌 조회 stub (정원10, 현재수강5, 학점3)
        stubCourse(10, 5, 3);

        doThrow(new CreditLimitExceededException())
                .when(enrollmentValidator)
                .ensureEnrollmentAllowed(profile.getId(), COURSE_ID);

        // 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(profile.getId(), COURSE_ID)
        ).isInstanceOf(CreditLimitExceededException.class);

        // 저장(save)이 호출되지 않아야 함
        verify(enrollmentRepository, never()).save(any());
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 실패 – 시간표 충돌 시")
    void enrollment_throws7() {
        // 0) Redis 카운터 stub (capacity 검증 통과용)
        stubRedisCounters(10L, 5L);

        // 1) 강좌 조회 stub (정원10, 현재수강5, 학점3)
        stubCourse(10, 5, 3);

        // 2) 스케줄 충돌 예외를 던지도록 validator stub
        doThrow(new ScheduleConflictException())
                .when(enrollmentValidator)
                .ensureEnrollmentAllowed(profile.getId(), COURSE_ID);

        // 3) 실행 및 예외 검증
        assertThatThrownBy(() ->
                enrollmentService.enrollment(profile.getId(), COURSE_ID)
        ).isInstanceOf(ScheduleConflictException.class);

        // 4) save 호출이 일어나지 않아야 함
        verify(enrollmentRepository, never()).save(any());
        verify(courseRepository, never()).save(any());
    }


    @Test
    @DisplayName("내 수강신청 기간 조회 - 성공")
    void getMyEnrollmentPeriod_success() {

        // given: today 기준으로 기간 안에 들어오는 start/end 날짜 설정
        EnrollmentPeriod period = createPeriod(1, 1);
        stubEnrollmentPeriod(period);
        when(enrollmentValidator.isWithinPeriod(any(), any())).thenReturn(true);
        // when
        StudentEnrollmentPeriodResponse response = enrollmentService.getMyEnrollmentPeriod(profile);

        // then
        assertThat(response.studentId()).isEqualTo(profile.getId());
        assertThat(response.universityName()).isEqualTo(profile.getUniversity().getName());
        assertThat(response.year()).isEqualTo(period.getYear());
        assertThat(response.grade()).isEqualTo(period.getGrade());
        assertThat(response.semester()).isEqualTo(period.getSemester());
        assertThat(response.startDate()).isEqualTo(period.getStartDate());
        assertThat(response.endDate()).isEqualTo(period.getEndDate());
        assertThat(response.isEnrollmentOpen()).isTrue();

    }

    @Test
    @DisplayName("내 수강신청 기간 조회 - 성공 - 아직 시작하지 않은 경우")
    void getMyEnrollmentPeriod_fail1() {

        // given: today 기준으로 기간 안에 들어오는 start/end 날짜 설정
        EnrollmentPeriod period = createNotYetPeriod(1, 2); // startDate = 내일, endDate = 모레

        stubEnrollmentPeriod(period); // 학생정보로 수강신청 기간 검색 시 만들어준 신청 기간이 반환되도록 stub

        // when
        StudentEnrollmentPeriodResponse response = enrollmentService.getMyEnrollmentPeriod(profile);

        // then
        assertThat(response.studentId()).isEqualTo(profile.getId());
        assertThat(response.universityName()).isEqualTo(profile.getUniversity().getName());
        assertThat(response.year()).isEqualTo(period.getYear());
        assertThat(response.grade()).isEqualTo(period.getGrade());
        assertThat(response.semester()).isEqualTo(period.getSemester());
        assertThat(response.startDate()).isEqualTo(period.getStartDate());
        assertThat(response.endDate()).isEqualTo(period.getEndDate());
        assertThat(response.isEnrollmentOpen()).isFalse();

    }

    @Test
    @DisplayName("내 수강신청 기간 조회 - 이미 지난 기간")
    void getMyEnrollmentPeriod_fail2() {
        // given: 오늘 기준 이미 지난 기간 (endDate < today)
        EnrollmentPeriod period = createPeriod(5, -1); // startDate = today.minusDays(5), endDate = today.minusDays(1)
        stubEnrollmentPeriod(period);

        // when
        StudentEnrollmentPeriodResponse response =
                enrollmentService.getMyEnrollmentPeriod(profile);

        // then: 기간 정보는 그대로 전달되지만 isEnrollmentOpen == false
        assertThat(response.studentId()).isEqualTo(profile.getId());
        assertThat(response.universityName()).isEqualTo(profile.getUniversity().getName());
        assertThat(response.year()).isEqualTo(period.getYear());
        assertThat(response.grade()).isEqualTo(period.getGrade());
        assertThat(response.semester()).isEqualTo(period.getSemester());
        assertThat(response.startDate()).isEqualTo(period.getStartDate());
        assertThat(response.endDate()).isEqualTo(period.getEndDate());
        assertThat(response.isEnrollmentOpen()).isFalse();
    }

    @Test
    @DisplayName("내 수강신청 기간 조회 - 실패 - 수강신청 기간 정보가 없는 경우")
    void getMyEnrollmentPeriod_fail3() {

        LocalDate today = LocalDate.now();

        when(enrollmentPeriodRepository
                .findByUniversityIdAndYearAndGradeAndSemester(
                        eq(profile.getUniversity().getId()),
                        eq(today.getYear()),
                        eq(profile.getGrade()),
                        eq(profile.getSemester())
                ))
                .thenReturn(Optional.empty());

        // when
        StudentEnrollmentPeriodResponse response = enrollmentService.getMyEnrollmentPeriod(profile);

        System.out.println(response.toString());

        // then: isEnrollmentOpen=false, 기타 기간 정보는 null
        assertThat(response.studentId()).isNull();
        assertThat(response.universityName()).isNull();
        assertThat(response.grade()).isNull();
        assertThat(response.semester()).isNull();
        assertThat(response.startDate()).isNull();
        assertThat(response.endDate()).isNull();
        assertThat(response.isEnrollmentOpen()).isFalse();

    }

}