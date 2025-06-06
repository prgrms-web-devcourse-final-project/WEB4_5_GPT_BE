package com.WEB4_5_GPT_BE.unihub.domain.course.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseRequest;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseScheduleDto;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseWithFullScheduleResponse;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseWithOutUrlRequest;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.CourseNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.LocationScheduleConflictException;
import com.WEB4_5_GPT_BE.unihub.domain.course.exception.ProfessorScheduleConflictException;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseScheduleRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.repository.EnrollmentRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.ProfessorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.MajorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import com.WEB4_5_GPT_BE.unihub.global.infra.s3.S3Service;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.catchThrowable;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;

@DisplayName("강의 도메인 서비스 레이어 테스트")
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseScheduleRepository courseScheduleRepository;

    @Mock
    private MajorRepository majorRepository;

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRedisCounterService courseRedisCounterService;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse1;
    private Course testCourse2;
    // testCourse1과 강의실 스케줄 중복
    private CourseRequest testCourseRequest1;
    // testCourse1과 교수 스케줄 중복
    private CourseRequest testCourseRequest2;
    // 스케줄 중복 없음
    private CourseRequest testCourseRequest3;
    private Major testMajor1;
    private Major testMajor2;
    private Professor testProfessorProfile1;
    private University testUniversity1;
    private SecurityUser testAuthUser1;

    @BeforeEach
    public void setUp() {
        testUniversity1 = new University(1L, "testUniversity1", "unihub.ac.kr");
        testMajor1 = new Major(1L, testUniversity1, "testMajor1");
        testMajor2 = new Major(2L, testUniversity1, "testMajor2");
        testUniversity1.getMajors().add(testMajor1);
        testUniversity1.getMajors().add(testMajor2);
        testCourse1 = new Course(1L, "testCourse1", testMajor1,
                "testLocation1", 50, 12, 4,
                null, 4, 1, null);
        testCourse2 = new Course(3L, "testCourse3", testMajor2,
                "testLocation3", 29, 1, 2,
                testProfessorProfile1, 1, 1, "/anotherPath/anotherImage.png");
        testCourse1.getSchedules().add(new CourseSchedule(
                1L, testCourse1, testUniversity1.getId(), testCourse1.getLocation(), null,
                DayOfWeek.MON, LocalTime.parse("12:00"), LocalTime.parse("14:00")));
        testCourse1.getSchedules().add(new CourseSchedule(
                1L, testCourse1, testUniversity1.getId(), testCourse1.getLocation(), null,
                DayOfWeek.WED, LocalTime.parse("12:00"), LocalTime.parse("14:00")));
        testCourse1.getSchedules().add(new CourseSchedule(
                1L, testCourse1, testUniversity1.getId(), testCourse1.getLocation(), null,
                DayOfWeek.FRI, LocalTime.parse("12:00"), LocalTime.parse("14:00")));
        testProfessorProfile1 = Professor.builder()
                .id(1L)
                .university(testUniversity1)
                .major(testMajor2)
                .password("testPassword1")
                .email("testProfEmail1@unihub.ac.kr")
                .employeeId("testEmpId1")
                .approvalStatus(ApprovalStatus.APPROVED)
                .name("testMember1")
                .build();
        testCourse2.setProfessor(testProfessorProfile1);
        testCourse1.getSchedules().forEach(cs -> cs.setProfessorProfileEmployeeId(testProfessorProfile1.getEmployeeId()));
        testCourseRequest1 = new CourseRequest("testCourseRequest1", testMajor2.getName(), testUniversity1.getName(),
                testCourse1.getLocation(), 40, 3, null, 4, 2, null,
                List.of(new CourseScheduleDto(DayOfWeek.MON, "13:00", "14:00")));
        testCourseRequest2 = new CourseRequest("testCourseRequest2", testMajor2.getName(), testUniversity1.getName(),
                "nonexistentLocation", 40, 3, testProfessorProfile1.getEmployeeId(), 4, 2, null,
                List.of(new CourseScheduleDto(DayOfWeek.MON, "13:00", "14:00")));
        testCourseRequest3 = new CourseRequest("testCourseRequest3", testMajor2.getName(), testUniversity1.getName(),
                "nonexistentLocation", 40, 3, testProfessorProfile1.getEmployeeId(), 4, 2, null,
                List.of(new CourseScheduleDto(DayOfWeek.TUE, "13:00", "14:00")));
        testAuthUser1 = new SecurityUser(testProfessorProfile1, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));
    }

    @Test
    @DisplayName("존재하는 강의를 ID로 조회했을때, 해당 강의를 반환한다.")
    void givenExistingCourseId_whenRequestingCourse_thenReturnCourse() {
        given(courseRepository.findById(testCourse1.getId())).willReturn(Optional.of(testCourse1));

        CourseWithFullScheduleResponse result = courseService.getCourseWithFullScheduleById(testCourse1.getId());

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo(testCourse1.getTitle());
        assertThat(result.schedule().size()).isEqualTo(testCourse1.getSchedules().size());
        then(courseRepository).should().findById(testCourse1.getId());
    }

    @Test
    @DisplayName("존재하지 않는 강의의 ID로 조회했을때, 예외를 던진다.")
    void givenNonexistentCourseId_whenRequestingCourse_thenReturnCourse() {
        given(courseRepository.findById(1234567L)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(
                () -> courseService.getCourseWithFullScheduleById(1234567L));

        assertThat(thrown)
                .isInstanceOf(UnihubException.class)
                .hasMessage("해당 강의가 존재하지 않습니다.");
        then(courseRepository).should().findById(1234567L);
    }

    @Test
    @DisplayName("스케줄이 없는 강의를 생성할때 정상 생성된다.")
    void givenCourseWithoutSchedule_whenCreatingCourse_thenSaveCourse() {

        doNothing().when(courseRedisCounterService).syncCourseCounters(any(Course.class));

        CourseRequest testCourseRequest = new CourseRequest("testCourseRequest3", testMajor2.getName(), testUniversity1.getName(),
                "nonexistentLocation", 40, 3, testProfessorProfile1.getEmployeeId(), 4, 2, null, List.of());
        given(professorRepository.findById(testProfessorProfile1.getId())).willReturn(Optional.of(testProfessorProfile1));
        given(universityRepository.findById(testUniversity1.getId())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        given(professorRepository.findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));
        given(courseRepository.save(any(Course.class))).willReturn(testCourseRequest.toEntity(testMajor2, 0, testProfessorProfile1));

        CourseWithFullScheduleResponse result = courseService.createCourse(testCourseRequest, testAuthUser1);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo(testCourseRequest.title());
        assertThat(result.schedule().size()).isEqualTo(testCourseRequest.schedule().size());
        then(universityRepository).should().findById(testUniversity1.getId());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest.major());
    }

    @Test
    @DisplayName("스케줄이 중복되지 않는 강의를 생성할때 정상 생성된다.")
    void givenCourseWithNonconflictingSchedule_whenCreatingCourse_thenSaveCourse() {

        doNothing().when(courseRedisCounterService).syncCourseCounters(any(Course.class));

        given(professorRepository.findById(testProfessorProfile1.getId())).willReturn(Optional.of(testProfessorProfile1));
        given(universityRepository.findById(testUniversity1.getId())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest3.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        given(professorRepository.findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));
        given(courseScheduleRepository.findByUniversityIdAndProfessorEmployeeId(
                testUniversity1.getId(),
                testProfessorProfile1.getEmployeeId()))
                .willReturn(testCourse1.getSchedules());
        given(courseRepository.save(any(Course.class))).willReturn(testCourseRequest3.toEntity(testMajor2, 0, testProfessorProfile1));

        CourseWithFullScheduleResponse result = courseService.createCourse(testCourseRequest3, testAuthUser1);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo(testCourseRequest3.title());
        assertThat(result.schedule().size()).isEqualTo(testCourseRequest3.schedule().size());
        then(universityRepository).should().findById(testUniversity1.getId());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest3.major());
        then(professorRepository).should().findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId());
        then(courseScheduleRepository).should().findByUniversityIdAndProfessorEmployeeId(
                testUniversity1.getId(),
                testProfessorProfile1.getEmployeeId());
        then(courseRepository).should().save(any(Course.class));
    }

    @Test
    @DisplayName("강의실 스케줄이 중복되는 강의를 생성할때 예외를 던진다.")
    void givenCourseWithLocationConflict_whenCreatingCourse_thenThrowException() {
        given(professorRepository.findById(testProfessorProfile1.getId())).willReturn(Optional.of(testProfessorProfile1));
        given(universityRepository.findById(testUniversity1.getId())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest1.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        given(courseScheduleRepository.findByUniversityIdAndLocation(
                testUniversity1.getId(),
                testCourseRequest1.location()))
                .willReturn(testCourse1.getSchedules());

        Throwable result = catchThrowable(() -> courseService.createCourse(testCourseRequest1, testAuthUser1));

        assertThat(result)
                .isInstanceOf(LocationScheduleConflictException.class)
                .hasMessage("강의 장소가 이미 사용 중입니다.");
        then(universityRepository).should().findById(testUniversity1.getId());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest1.major());
        then(courseScheduleRepository).should().findByUniversityIdAndLocation(
                testUniversity1.getId(),
                testCourseRequest1.location());
    }

    @Test
    @DisplayName("교수 스케줄이 중복되는 강의를 생성할때 예외를 던진다.")
    void givenCourseWithProfessorConflict_whenCreatingCourse_thenThrowException() {
        given(professorRepository.findById(testProfessorProfile1.getId())).willReturn(Optional.of(testProfessorProfile1));
        given(universityRepository.findById(testUniversity1.getId())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest2.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        given(professorRepository.findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));
        given(courseScheduleRepository.findByUniversityIdAndProfessorEmployeeId(
                testUniversity1.getId(),
                testCourseRequest2.employeeId()))
                .willReturn(testCourse1.getSchedules());

        Throwable result = catchThrowable(() -> courseService.createCourse(testCourseRequest2, testAuthUser1));

        assertThat(result)
                .isInstanceOf(UnihubException.class)
                .hasMessage("강사/교수가 이미 수업 중입니다.");
        then(universityRepository).should().findById(testUniversity1.getId());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest1.major());
        then(professorRepository).should().findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId());
        then(courseScheduleRepository).should().findByUniversityIdAndProfessorEmployeeId(
                testUniversity1.getId(),
                testCourseRequest2.employeeId());
    }

    @Test
    @DisplayName("존재하지 않는 강의의 ID를 수정하려 했을때, 예외를 던진다.")
    void givenNonexistentCourseId_whenUpdatingCourse_thenThrowException() {
        given(courseRepository.findById(1234567L)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(
                () -> courseService.updateCourse(1234567L, testCourseRequest1, testAuthUser1));

        assertThat(thrown)
                .isInstanceOf(CourseNotFoundException.class)
                .hasMessage("해당 강의가 존재하지 않습니다.");
        then(courseRepository).should().findById(1234567L);
    }

    @Test
    @DisplayName("스케줄이 없는 정보로 강의를 수정할때 정상 수정된다.")
    void givenCourseWithoutSchedule_whenUpdatingCourse_thenUpdateCourse() {
        CourseRequest testCourseRequest = new CourseRequest("testCourseRequest3", testMajor2.getName(), testUniversity1.getName(),
                "nonexistentLocation", 40, 3, testProfessorProfile1.getEmployeeId(), 4, 2, null, List.of());
        given(courseRepository.findById(1L)).willReturn(Optional.of(testCourseRequest.toEntity(testMajor2, 0, testProfessorProfile1)));
        given(professorRepository.findById(testProfessorProfile1.getId())).willReturn(Optional.of(testProfessorProfile1));
        given(universityRepository.findById(testUniversity1.getId())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest.major()))
                .willReturn(Optional.of(testMajor2));
        given(professorRepository.findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));
        given(courseRepository.save(any(Course.class))).willReturn(testCourseRequest.toEntity(testMajor2, 0, testProfessorProfile1));

        CourseWithFullScheduleResponse result = courseService.updateCourse(1L, testCourseRequest, testAuthUser1);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo(testCourseRequest.title());
        assertThat(result.schedule().size()).isEqualTo(testCourseRequest.schedule().size());
        then(courseRepository).should().findById(1L);
        then(universityRepository).should().findById(testUniversity1.getId());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest.major());
        then(professorRepository).should().findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId());
        then(courseRepository).should().save(any(Course.class));
    }

    @Test
    @DisplayName("스케줄이 중복되지 않는 정보로 강의를 수정할때 정상 생성된다.")
    void givenCourseWithNonconflictingSchedule_whenUpdatingCourse_thenUpdateCourse() {
        given(courseRepository.findById(1L)).willReturn(Optional.of(testCourse1));
        given(professorRepository.findById(testProfessorProfile1.getId())).willReturn(Optional.of(testProfessorProfile1));
        given(universityRepository.findById(testUniversity1.getId())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest3.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        given(professorRepository.findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));
        given(courseScheduleRepository.findByUniversityIdAndLocationExcludingCourse(
                testUniversity1.getId(),
                testCourseRequest3.location(),
                1L))
                .willReturn(testCourse1.getSchedules());
        given(courseScheduleRepository.findByUniversityIdAndProfessorEmployeeIdExcludingCourse(
                testUniversity1.getId(),
                testProfessorProfile1.getEmployeeId(),
                1L))
                .willReturn(testCourse1.getSchedules());
        given(courseRepository.save(any(Course.class))).willReturn(testCourseRequest3.toEntity(testMajor2, 0, testProfessorProfile1));

        CourseWithFullScheduleResponse result = courseService.updateCourse(1L, testCourseRequest3, testAuthUser1);

        assertThat(result).isNotNull();
        assertThat(result.id()).isNull();
        assertThat(result.title()).isEqualTo(testCourseRequest3.title());
        then(courseRepository).should().findById(1L);
        then(universityRepository).should().findById(testUniversity1.getId());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest3.major());
        then(professorRepository).should().findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId());
        then(courseScheduleRepository).should().findByUniversityIdAndLocationExcludingCourse(
                testUniversity1.getId(),
                testCourseRequest3.location(),
                1L);
        then(courseScheduleRepository).should().findByUniversityIdAndProfessorEmployeeIdExcludingCourse(
                testUniversity1.getId(),
                testProfessorProfile1.getEmployeeId(),
                1L);
        then(courseRepository).should().save(any(Course.class));
    }

    @Test
    @DisplayName("강의실 스케줄이 중복되는 정보로 강의를 수정할때 예외를 던진다.")
    void givenCourseWithLocationConflict_whenUpdatingCourse_thenThrowException() {
        given(courseRepository.findById(testCourse1.getId())).willReturn(Optional.of(testCourse1));
        given(professorRepository.findById(testProfessorProfile1.getId())).willReturn(Optional.of(testProfessorProfile1));
        given(universityRepository.findById(testUniversity1.getId())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest1.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        given(courseScheduleRepository.findByUniversityIdAndLocationExcludingCourse(
                testUniversity1.getId(),
                testCourseRequest1.location(),
                testCourse1.getId()))
                .willReturn(testCourse1.getSchedules());

        Throwable result = catchThrowable(() -> courseService.updateCourse(testCourse1.getId(), testCourseRequest1, testAuthUser1));

        assertThat(result)
                .isInstanceOf(LocationScheduleConflictException.class)
                .hasMessage("강의 장소가 이미 사용 중입니다.");
        then(courseRepository).should().findById(testCourse1.getId());
        then(universityRepository).should().findById(testUniversity1.getId());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest1.major());
        then(courseScheduleRepository).should().findByUniversityIdAndLocationExcludingCourse(
                testUniversity1.getId(),
                testCourseRequest1.location(),
                testCourse1.getId());
    }

    @Test
    @DisplayName("교수 스케줄이 중복되는 정보로 강의를 수정할때 예외를 던진다.")
    void givenCourseWithProfessorConflict_whenUpdatingCourse_thenThrowException() {
        given(courseRepository.findById(testCourse1.getId())).willReturn(Optional.of(testCourse1));
        given(professorRepository.findById(testProfessorProfile1.getId())).willReturn(Optional.of(testProfessorProfile1));
        given(universityRepository.findById(testUniversity1.getId())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest2.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        given(professorRepository.findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));
        given(courseScheduleRepository.findByUniversityIdAndProfessorEmployeeIdExcludingCourse(
                testUniversity1.getId(),
                testCourseRequest2.employeeId(),
                testCourse1.getId()))
                .willReturn(testCourse1.getSchedules());

        Throwable result = catchThrowable(() -> courseService.updateCourse(testCourse1.getId(), testCourseRequest2, testAuthUser1));

        assertThat(result)
                .isInstanceOf(ProfessorScheduleConflictException.class)
                .hasMessage("강사/교수가 이미 수업 중입니다.");
        then(courseRepository).should().findById(testCourse1.getId());
        then(universityRepository).should().findById(testUniversity1.getId());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest1.major());
        then(professorRepository).should().findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId());
        then(courseScheduleRepository).should().findByUniversityIdAndProfessorEmployeeIdExcludingCourse(
                testUniversity1.getId(),
                testCourseRequest2.employeeId(),
                testCourse1.getId());
    }

    // 나머지 2가지 검색 타입에 대한 테스트 생략
    @Test
    @DisplayName("검색어(제목, 교수명, 전공, 학년, 학기)와 페이지네이션 정보로 강의 목록을 조회한다.")
    void givenSearchConditionsAndPagination_whenRequestingCourseList_thenReturnFilteredCourseList() {
        // given
        PageRequest pageable = PageRequest.of(0, 5, Sort.by("credit").descending());
        Page<Course> page = new PageImpl<>(
                List.of(testCourse1, testCourse2),
                pageable,
                2
        );

        Long majorId = 1L;
        Integer grade = 2;
        Integer semester = 1;

        ArgumentCaptor<Long> universityIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> profNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> majorIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> gradeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> semesterCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        given(professorRepository.findById(testProfessorProfile1.getId()))
                .willReturn(Optional.of(testProfessorProfile1));
        given(courseRepository.findWithFilters(
                universityIdCaptor.capture(),
                titleCaptor.capture(),
                profNameCaptor.capture(),
                majorIdCaptor.capture(),
                gradeCaptor.capture(),
                semesterCaptor.capture(),
                pageableCaptor.capture()
        )).willReturn(page);

        // when
        Page<CourseWithFullScheduleResponse> result = courseService.findAllCoursesModeFull(
                "검색제목", "교수이름", majorId, grade, semester, testAuthUser1, pageable
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNumberOfElements()).isEqualTo(2);
        assertThat(universityIdCaptor.getValue()).isEqualTo(testProfessorProfile1.getUniversity().getId());
        assertThat(titleCaptor.getValue()).isEqualTo("검색제목");
        assertThat(profNameCaptor.getValue()).isEqualTo("교수이름");
        assertThat(majorIdCaptor.getValue()).isEqualTo(majorId);
        assertThat(gradeCaptor.getValue()).isEqualTo(grade);
        assertThat(semesterCaptor.getValue()).isEqualTo(semester);
        assertThat(pageableCaptor.getValue()).isEqualTo(pageable);

        then(professorRepository).should().findById(testProfessorProfile1.getId());
        then(courseRepository).should().findWithFilters(
                anyLong(), anyString(), anyString(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("존재하는 강의는 S3 파일 삭제 후 DB에서 삭제된다")
    void deleteCourse_withAttachment_deletesFileAndCourse() {
        // given
        Long courseId = 42L;
        University uni = new University(1L, "U", "u.ac.kr");
        Major maj = new Major(2L, uni, "DummyMajor");
        Course course = Course.builder()
                .id(courseId)
                .title("삭제용 강의")
                .major(maj)
                .location("TestRoom")
                .capacity(10)
                .enrolled(5)
                .credit(3)
                .professor(null)
                .grade(1)
                .semester(1)
                .coursePlanAttachment("/some/bucket/path.pdf")
                .build();

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        // ↳ 수강신청이 하나도 없다고 강제
        given(enrollmentRepository.existsByCourseId(courseId)).willReturn(false);

        doNothing().when(courseRedisCounterService).deleteCourseCounters(any(Course.class));

        // when
        courseService.deleteCourse(courseId);

        // then
        then(s3Service).should().deleteByUrl("/some/bucket/path.pdf");
        then(courseRepository).should().delete(course);
    }


    @Test
    @DisplayName("강의가 존재하지 않으면 404 예외 발생")
    void deleteCourse_notFound_throwsException() {
        given(courseRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> courseService.deleteCourse(99L))
                .isInstanceOfSatisfying(UnihubException.class, ex ->
                        assertThat(ex.getCode())
                                .isEqualTo(String.valueOf(HttpStatus.NOT_FOUND.value()))
                );
    }

    @Test
    @DisplayName("강의 수정시, 파일이 있으면 기존 S3 URL 삭제 후 새로 업로드된 URL이 반환된다")
    void givenFile_whenUpdatingCourse_thenUploadNewAndDeleteOldAttachment() throws IOException {
        // given
        Long courseId = 1L;
        String oldUrl = "/old/bucket/plan.pdf";
        String newUrl = "https://bucket-1.s3.ap-northeast-2.amazonaws.com/12345_new_plan.pdf";

        // 원본 Course 엔티티(기존 URL 세팅)
        Course origin = Course.builder()
                .id(courseId)
                .title("originalTitle")
                .major(testMajor1)
                .location("origLocation")
                .capacity(30)
                .enrolled(10)
                .credit(3)
                .professor(testProfessorProfile1)
                .grade(1)
                .semester(1)
                .coursePlanAttachment(oldUrl)
                .build();
        origin.getSchedules().addAll(testCourse1.getSchedules()); // 스케줄은 충돌 없음

        given(courseRepository.findById(courseId)).willReturn(Optional.of(origin));
        given(professorRepository.findById(testProfessorProfile1.getId())).willReturn(Optional.of(testProfessorProfile1));
        given(universityRepository.findById(testUniversity1.getId())).willReturn(Optional.of(testUniversity1));
        given(s3Service.upload(any(MultipartFile.class))).willReturn(newUrl);

        // 충돌 검사: 모두 비어있어야 통과
        given(courseScheduleRepository.findByUniversityIdAndLocationExcludingCourse(
                eq(testUniversity1.getId()),
                anyString(),
                eq(courseId))
        ).willReturn(List.of());
        given(courseScheduleRepository.findByUniversityIdAndProfessorEmployeeIdExcludingCourse(
                eq(testUniversity1.getId()),
                anyString(),
                eq(courseId))
        ).willReturn(List.of());

        // 대학/전공/교수 조회
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testMajor1.getName()))
                .willReturn(Optional.of(testMajor1));
        given(professorRepository.findByUniversityIdAndEmployeeId(
                testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));

        // save 후에 coursePlanAttachment가 newUrl인 객체 리턴
        Course saved = Course.builder()
                .id(courseId)
                .title(origin.getTitle())
                .major(origin.getMajor())
                .location(origin.getLocation())
                .capacity(origin.getCapacity())
                .enrolled(origin.getEnrolled())
                .credit(origin.getCredit())
                .professor(origin.getProfessor())
                .grade(origin.getGrade())
                .semester(origin.getSemester())
                .coursePlanAttachment(newUrl)
                .build();
        saved.getSchedules().addAll(origin.getSchedules());
        given(courseRepository.save(any(Course.class))).willReturn(saved);

        // request DTO 준비 (기존 데이터를 그대로 담고, attachment 필드만 바꿀 준비)
        CourseWithOutUrlRequest req = new CourseWithOutUrlRequest(
                origin.getTitle(),
                origin.getMajor().getName(),
                origin.getMajor().getUniversity().getName(),
                origin.getLocation(),
                origin.getCapacity(),
                origin.getCredit(),
                origin.getProfessor().getEmployeeId(),
                origin.getGrade(),
                origin.getSemester(),
                origin.getSchedules().stream()
                        .map(CourseScheduleDto::from)
                        .toList()
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "new_plan.pdf",
                "application/pdf",
                "dummy-content".getBytes()
        );

        // when
        CourseWithFullScheduleResponse result = courseService.updateCourse(courseId, req, file, testAuthUser1);

        // then
        assertThat(result).isNotNull();
        assertThat(result.coursePlanAttachment()).isEqualTo(newUrl);

        // 기존 URL 삭제, 새 파일 업로드 호출 검증
        then(s3Service).should().upload(file);
        then(s3Service).should().deleteByUrl(oldUrl);

        // 최종 저장까지 호출됐는지 확인
        then(courseRepository).should().save(any(Course.class));
    }
}
