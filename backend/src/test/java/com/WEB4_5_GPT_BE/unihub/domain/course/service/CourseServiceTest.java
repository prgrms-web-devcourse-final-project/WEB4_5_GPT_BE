package com.WEB4_5_GPT_BE.unihub.domain.course.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseRequest;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseScheduleDto;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseWithFullScheduleResponse;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseScheduleRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.ProfessorProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.StudentProfileRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.MajorRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.repository.UniversityRepository;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.catchThrowable;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private ProfessorProfileRepository professorProfileRepository;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse1;
    private Course testCourse2;
    private Course testCourse3;
    // testCourse1과 강의실 스케줄 중복
    private CourseRequest testCourseRequest1;
    // testCourse1과 교수 스케줄 중복
    private CourseRequest testCourseRequest2;
    // 스케줄 중복 없음
    private CourseRequest testCourseRequest3;
    private Major testMajor1;
    private Major testMajor2;
    private ProfessorProfile testProfessorProfile1;
    private University testUniversity1;
    private Member testMember1;

    @BeforeEach
    public void setUp() {
        testMember1 = new Member(1L, "testEmail@company.com", "testPassword1",
                "testMember1", Role.PROFESSOR, false, null, null, null);
        testUniversity1 = new University(1L, "testUniversity1");
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
        testCourse3 = new Course(3L, "testCourse3", testMajor2,
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
        testProfessorProfile1 = new ProfessorProfile(1L, testMember1, "testEmpId1",
                testUniversity1, testMajor2, ApprovalStatus.APPROVED);
        testCourse2.setProfessor(testProfessorProfile1);
        testCourse1.getSchedules().forEach(cs -> cs.setProfessorProfileEmployeeId(testProfessorProfile1.getEmployeeId()));
        testMember1.setProfessorProfile(testProfessorProfile1);
        testCourseRequest1 = new CourseRequest("testCourseRequest1", testMajor2.getName(), testUniversity1.getName(),
                testCourse1.getLocation(), 40, 3, null, 4, 2, null,
                List.of(new CourseScheduleDto("MON", "13:00", "14:00")));
        testCourseRequest2 = new CourseRequest("testCourseRequest2", testMajor2.getName(), testUniversity1.getName(),
                "nonexistentLocation", 40, 3, testProfessorProfile1.getEmployeeId(), 4, 2, null,
                List.of(new CourseScheduleDto("MON", "13:00", "14:00")));
        testCourseRequest3 = new CourseRequest("testCourseRequest3", testMajor2.getName(), testUniversity1.getName(),
                "nonexistentLocation", 40, 3, testProfessorProfile1.getEmployeeId(), 4, 2, null,
                List.of(new CourseScheduleDto("TUE", "13:00", "14:00")));
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
        CourseRequest testCourseRequest = new CourseRequest("testCourseRequest3", testMajor2.getName(), testUniversity1.getName(),
                "nonexistentLocation", 40, 3, testProfessorProfile1.getEmployeeId(), 4, 2, null, List.of());
        given(universityRepository.findByName(testUniversity1.getName())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        given(professorProfileRepository.findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));
        given(courseRepository.save(any(Course.class))).willReturn(testCourseRequest.toEntity(testMajor2, 0, testProfessorProfile1));

        CourseWithFullScheduleResponse result = courseService.createCourse(testCourseRequest);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo(testCourseRequest.title());
        assertThat(result.schedule().size()).isEqualTo(testCourseRequest.schedule().size());
        then(universityRepository).should().findByName(testUniversity1.getName());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest.major());
    }

    @Test
    @DisplayName("스케줄이 중복되지 않는 강의를 생성할때 정상 생성된다.")
    void givenCourseWithNonconflictingSchedule_whenCreatingCourse_thenSaveCourse() {
        given(universityRepository.findByName(testUniversity1.getName())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest3.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        CourseScheduleDto csd = testCourseRequest3.schedule().getFirst();given(professorProfileRepository.findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));
        given(courseScheduleRepository.existsByUnivIdAndLocationAndDayOfWeek(
                    testUniversity1.getId(),
                    testCourseRequest3.location(),
                    csd.day(),
                    LocalTime.parse(csd.startTime()),
                    LocalTime.parse(csd.endTime())))
                .willReturn(false);
        given(courseScheduleRepository.existsByProfEmpIdAndDayOfWeek(
                    testProfessorProfile1.getEmployeeId(),
                    csd.day(),
                    LocalTime.parse(csd.startTime()),
                    LocalTime.parse(csd.endTime())))
                .willReturn(false);
        given(courseRepository.save(any(Course.class))).willReturn(testCourseRequest3.toEntity(testMajor2, 0, testProfessorProfile1));

        CourseWithFullScheduleResponse result = courseService.createCourse(testCourseRequest3);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo(testCourseRequest3.title());
        assertThat(result.schedule().size()).isEqualTo(testCourseRequest3.schedule().size());
        then(universityRepository).should().findByName(testUniversity1.getName());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest3.major());
        then(courseScheduleRepository).should().existsByUnivIdAndLocationAndDayOfWeek(
                testUniversity1.getId(),
                testCourseRequest3.location(),
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime()));
    }

    @Test
    @DisplayName("강의실 스케줄이 중복되는 강의를 생성할때 예외를 던진다.")
    void givenCourseWithLocationConflict_whenCreatingCourse_thenThrowException() {
        given(universityRepository.findByName(testUniversity1.getName())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest1.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        CourseScheduleDto csd = testCourseRequest1.schedule().getFirst();
        given(courseScheduleRepository.existsByUnivIdAndLocationAndDayOfWeek(
                testUniversity1.getId(),
                testCourseRequest1.location(),
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime())))
                .willReturn(true);

        Throwable result = catchThrowable(() -> courseService.createCourse(testCourseRequest1));

        assertThat(result)
                .isInstanceOf(UnihubException.class)
                .hasMessage("강의 장소가 이미 사용 중입니다.");
        then(universityRepository).should().findByName(testUniversity1.getName());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest1.major());
        then(courseScheduleRepository).should().existsByUnivIdAndLocationAndDayOfWeek(
                testUniversity1.getId(),
                testCourseRequest1.location(),
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime()));
    }

    @Test
    @DisplayName("교수 스케줄이 중복되는 강의를 생성할때 예외를 던진다.")
    void givenCourseWithProfessorConflict_whenCreatingCourse_thenThrowException() {
        given(universityRepository.findByName(testUniversity1.getName())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest2.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        CourseScheduleDto csd = testCourseRequest2.schedule().getFirst();given(professorProfileRepository.findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));
        given(courseScheduleRepository.existsByProfEmpIdAndDayOfWeek(
                testCourseRequest2.employeeId(),
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime())))
                .willReturn(true);


        Throwable result = catchThrowable(() -> courseService.createCourse(testCourseRequest2));

        assertThat(result)
                .isInstanceOf(UnihubException.class)
                .hasMessage("강사/교수가 이미 수업 중입니다.");
        then(universityRepository).should().findByName(testUniversity1.getName());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest1.major());
        then(courseScheduleRepository).should().existsByProfEmpIdAndDayOfWeek(
                testCourseRequest2.employeeId(),
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime()));
    }

    @Test
    @DisplayName("존재하지 않는 강의의 ID를 수정하려 했을때, 예외를 던진다.")
    void givenNonexistentCourseId_whenUpdatingCourse_thenThrowException() {
        given(courseRepository.findById(1234567L)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(
                () -> courseService.updateCourse(1234567L, testCourseRequest1));

        assertThat(thrown)
                .isInstanceOf(UnihubException.class)
                .hasMessage("해당 강의가 존재하지 않습니다.");
        then(courseRepository).should().findById(1234567L);
    }

    @Test
    @DisplayName("스케줄이 없는 정보로 강의를 수정할때 정상 수정된다.")
    void givenCourseWithoutSchedule_whenUpdatingCourse_thenUpdateCourse() {
        CourseRequest testCourseRequest = new CourseRequest("testCourseRequest3", testMajor2.getName(), testUniversity1.getName(),
                "nonexistentLocation", 40, 3, testProfessorProfile1.getEmployeeId(), 4, 2, null, List.of());
        given(courseRepository.findById(1L)).willReturn(Optional.of(testCourseRequest.toEntity(testMajor2, 0, testProfessorProfile1)));
        given(universityRepository.findByName(testUniversity1.getName())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest.major()))
                .willReturn(Optional.of(testMajor2));
        given(professorProfileRepository.findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));
        given(courseRepository.save(any(Course.class))).willReturn(testCourseRequest.toEntity(testMajor2, 0, testProfessorProfile1));

        CourseWithFullScheduleResponse result = courseService.updateCourse(1L, testCourseRequest);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo(testCourseRequest.title());
        assertThat(result.schedule().size()).isEqualTo(testCourseRequest.schedule().size());
        then(universityRepository).should().findByName(testUniversity1.getName());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest.major());
    }

    @Test
    @DisplayName("스케줄이 중복되지 않는 정보로 강의를 수정할때 정상 생성된다.")
    void givenCourseWithNonconflictingSchedule_whenUpdatingCourse_thenUpdateCourse() {
        given(courseRepository.findById(1L)).willReturn(Optional.of(testCourse1));
        given(universityRepository.findByName(testUniversity1.getName())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest3.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        given(professorProfileRepository.findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));
        CourseScheduleDto csd = testCourseRequest3.schedule().getFirst();
        given(courseScheduleRepository.existsByUnivIdAndLocationAndDayOfWeek(
                testUniversity1.getId(),
                testCourseRequest3.location(),
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime())))
                .willReturn(false);
        given(courseScheduleRepository.existsByProfEmpIdAndDayOfWeek(
                testProfessorProfile1.getEmployeeId(),
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime())))
                .willReturn(false);
        given(courseRepository.save(any(Course.class))).willReturn(testCourseRequest3.toEntity(testMajor2, 0, testProfessorProfile1));

        CourseWithFullScheduleResponse result = courseService.updateCourse(1L, testCourseRequest3);

        assertThat(result).isNotNull();
        assertThat(result.id()).isNull();
        assertThat(result.title()).isEqualTo(testCourseRequest3.title());
        then(courseRepository).should().findById(1L);
        then(universityRepository).should().findByName(testUniversity1.getName());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest3.major());
        then(courseScheduleRepository).should().existsByUnivIdAndLocationAndDayOfWeek(
                testUniversity1.getId(),
                testCourseRequest3.location(),
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime()));
    }

    @Test
    @DisplayName("강의실 스케줄이 중복되는 정보로 강의를 수정할때 예외를 던진다.")
    void givenCourseWithLocationConflict_whenUpdatingCourse_thenThrowException() {
        given(courseRepository.findById(testCourse1.getId())).willReturn(Optional.of(testCourse1));
        given(universityRepository.findByName(testUniversity1.getName())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest1.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        CourseScheduleDto csd = testCourseRequest1.schedule().getFirst();
        given(courseScheduleRepository.existsByUnivIdAndLocationAndDayOfWeek(
                testUniversity1.getId(),
                testCourseRequest1.location(),
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime())))
                .willReturn(true);

        Throwable result = catchThrowable(() -> courseService.updateCourse(testCourse1.getId(), testCourseRequest1));

        assertThat(result)
                .isInstanceOf(UnihubException.class)
                .hasMessage("강의 장소가 이미 사용 중입니다.");
        then(universityRepository).should().findByName(testUniversity1.getName());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest1.major());
        then(courseScheduleRepository).should().existsByUnivIdAndLocationAndDayOfWeek(
                testUniversity1.getId(),
                testCourseRequest1.location(),
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime()));
    }

    @Test
    @DisplayName("교수 스케줄이 중복되는 정보로 강의를 수정할때 예외를 던진다.")
    void givenCourseWithProfessorConflict_whenUpdatingCourse_thenThrowException() {
        given(courseRepository.findById(testCourse1.getId())).willReturn(Optional.of(testCourse1));
        given(universityRepository.findByName(testUniversity1.getName())).willReturn(Optional.of(testUniversity1));
        given(majorRepository.findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest2.major()))
                .willReturn(Optional.of(testCourse2.getMajor()));
        CourseScheduleDto csd = testCourseRequest2.schedule().getFirst();given(professorProfileRepository.findByUniversityIdAndEmployeeId(testUniversity1.getId(), testProfessorProfile1.getEmployeeId()))
                .willReturn(Optional.of(testProfessorProfile1));
        given(courseScheduleRepository.existsByProfEmpIdAndDayOfWeek(
                testCourseRequest2.employeeId(),
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime())))
                .willReturn(true);


        Throwable result = catchThrowable(() -> courseService.updateCourse(testCourse1.getId(), testCourseRequest2));

        assertThat(result)
                .isInstanceOf(UnihubException.class)
                .hasMessage("강사/교수가 이미 수업 중입니다.");
        then(universityRepository).should().findByName(testUniversity1.getName());
        then(majorRepository).should().findByUniversityIdAndName(testUniversity1.getId(), testCourseRequest1.major());
        then(courseScheduleRepository).should().existsByProfEmpIdAndDayOfWeek(
                testCourseRequest2.employeeId(),
                csd.day(),
                LocalTime.parse(csd.startTime()),
                LocalTime.parse(csd.endTime()));
    }

    // 나머지 2가지 검색 타입에 대한 테스트 생략
    @Test
    @DisplayName("검색어(제목, 교수명)와 페이지네이션 정보로 강의를 목록 조회한다.")
    void givenSearchKeywordsAndPaginationObject_whenRequestingCourseList_thenReturnCourseList() {
        PageRequest pageable = PageRequest.of(0, 5, Sort.by("credit").descending());
        Page<Course> page = new PageImpl<>(
                List.of(testCourse1, testCourse2, testCourse3),
                pageable,
                12
        );
        SecurityUser authUser = new SecurityUser(testMember1, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));
        ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);
        given(professorProfileRepository.findByMemberId(testMember1.getId())).willReturn(Optional.of(testProfessorProfile1));
        given(courseRepository.findByTitleLikeAndProfessorNameLike(longCaptor.capture(), anyString(), anyString(), any(Pageable.class)))
                .willReturn(page);

        Page<CourseWithFullScheduleResponse> result = courseService.findAllCoursesModeFull("", "", authUser, pageable);
        Long captured = longCaptor.getValue();

        assertThat(result).isInstanceOf(Page.class);
        assertThat(result.getNumberOfElements()).isEqualTo(3);
        assertThat(captured).isEqualTo(testProfessorProfile1.getUniversity().getId());
        then(professorProfileRepository).should().findByMemberId(testMember1.getId());
        then(courseRepository).should().findByTitleLikeAndProfessorNameLike(anyLong(), anyString(), anyString(), any(Pageable.class));
    }
}
