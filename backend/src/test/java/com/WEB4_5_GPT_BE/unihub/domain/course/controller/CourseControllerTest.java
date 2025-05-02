package com.WEB4_5_GPT_BE.unihub.domain.course.controller;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseRequest;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseWithFullScheduleResponse;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.service.CourseService;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.AuthTokenService;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.global.Rq;
import com.WEB4_5_GPT_BE.unihub.global.security.CustomAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({CourseService.class,
        Rq.class,
        CustomAuthenticationFilter.class,
        AuthTokenService.class})
@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private Rq rq;

    @MockitoBean
    private CustomAuthenticationFilter customAuthenticationFilter;

    @MockitoBean
    private AuthTokenService authTokenService;

    private University testUniversity = new University(5L,
            "testUniversity");

    private Major testMajor = new Major(3L,
            testUniversity,
            "testMajor");

    private Course testCourse = new Course(1L,
            "testCourse",
            testMajor,
            "testLocation",
            40,
            4,
            4,
            null,
            4,
            1,
            "/somePath/testAttachment.jpg");

    private CourseSchedule testCourseSchedule1 = new CourseSchedule(1L,
            testCourse,
            123L,
            testCourse.getLocation(),
            null,
            DayOfWeek.MON,
            LocalTime.parse("12:00"),
            LocalTime.parse("13:00"));

    private CourseSchedule testCourseSchedule2 = new CourseSchedule(2L,
            testCourse,
            123L,
            testCourse.getLocation(),
            null,
            DayOfWeek.WED,
            LocalTime.parse("12:00"),
            LocalTime.parse("13:00"));

    @BeforeEach
    void setUp() {
        testCourse.getSchedules().add(testCourseSchedule1);
        testCourse.getSchedules().add(testCourseSchedule2);
    }

    @Test
    @DisplayName("강의 ID로 단건 조회 요청시 성공.")
    void givenCourseId_whenRequestingCourse_thenReturnCourse() throws Exception {
        Long courseId = 1L;
        ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);
        given(courseService.getCourseWithFullScheduleById(longCaptor.capture()))
                .willReturn(CourseWithFullScheduleResponse.from(testCourse));

        ResultActions resultActions = mockMvc.perform(get("/api/courses/%d".formatted(courseId)));
        Long captured = longCaptor.getValue();

        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(CourseController.class))
                .andExpect(handler().methodName("getCourse"))
                .andExpect(jsonPath("$.message").value("성공적으로 조회되었습니다."))
                .andExpect(jsonPath("$.data.title").value(testCourse.getTitle()));
        assertThat(captured).isEqualTo(courseId);
        then(courseService).should().getCourseWithFullScheduleById(any(Long.class));
    }

    @Test
    @DisplayName("정상적인 강의 정보로 생성 요청시 성공.")
    void givenValidCourseRequest_whenCreatingCourse_thenReturnCreatedCourse() throws Exception {
        given(courseService.createCourse(any(CourseRequest.class)))
                .willReturn(CourseWithFullScheduleResponse.from(testCourse));

        ResultActions resultActions = mockMvc.perform(post("/api/courses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(CourseRequest.from(testCourse))));

        resultActions
                // 테스트 실행시 필요한 빈만 로드되기 때문에 AOP가 동작하지 않음. 실제 응답은 정상적으로 201 Created 코드가 들어감
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(CourseController.class))
                .andExpect(handler().methodName("createCourse"))
                .andExpect(jsonPath("$.message").value("성공적으로 생성되었습니다."))
                .andExpect(jsonPath("$.data.title").value(testCourse.getTitle()))
                .andExpect(jsonPath("$.data.schedule", hasSize(2)))
                .andExpect(jsonPath("$.data.schedule[0].day").value(testCourse.getSchedules().getFirst().getDay().toString()));
        then(courseService).should().createCourse(any(CourseRequest.class));
    }

    @Test
    @DisplayName("강의 ID와 정상적인 강의 정보로 수정 요청시 성공.")
    void givenCourseIdAndValidCourseRequest_whenUpdatingCourse_thenReturnUpdatedCourse() throws Exception {
        Long courseId = 1L;
        ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);
        given(courseService.updateCourse(longCaptor.capture(), any(CourseRequest.class)))
                .willReturn(CourseWithFullScheduleResponse.from(testCourse));

        ResultActions resultActions = mockMvc.perform(put("/api/courses/%d".formatted(courseId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CourseRequest.from(testCourse)))
                .characterEncoding("utf-8"));
        Long captured = longCaptor.getValue();

        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(CourseController.class))
                .andExpect(handler().methodName("updateCourse"))
                .andExpect(jsonPath("$.message").value("성공적으로 수정되었습니다."))
                .andExpect(jsonPath("$.data.title").value(testCourse.getTitle()))
                .andExpect(jsonPath("$.data.schedule", hasSize(2)))
                .andExpect(jsonPath("$.data.schedule[0].day").value(testCourse.getSchedules().getFirst().getDay().toString()));
        assertThat(captured).isEqualTo(courseId);
        then(courseService).should().updateCourse(any(Long.class), any(CourseRequest.class));
    }

    @Test
    @DisplayName("강의 ID로 삭제 요청시 성공.")
    void givenCourseId_whenDeletingCourse_thenReturnOk() throws Exception {
        Long courseId = 1L;
        ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);
        doNothing().when(courseService).deleteCourse(longCaptor.capture());

        ResultActions resultActions = mockMvc.perform(delete("/api/courses/%d".formatted(courseId)));
        Long captured = longCaptor.getValue();

        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(CourseController.class))
                .andExpect(handler().methodName("deleteCourse"))
                .andExpect(jsonPath("$.message").value("성공적으로 삭제되었습니다."));
        assertThat(captured).isEqualTo(courseId);
        then(courseService).should().deleteCourse(any(Long.class));
    }

    @Test
    @DisplayName("강의 목록 조회 요청시 성공.")
    void givenQueryParams_whenRequestingCourseList_thenReturnCourseList() throws Exception {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(courseService.findAllCoursesModeFull(anyString(), anyString(), any(), pageableCaptor.capture()))
                .willReturn(new PageImpl<>(List.of()));
        // TODO: Enum 대소문자 구분
        ResultActions resultActions = mockMvc.perform(get("/api/courses?mode=FULL&sort=credit,desc"));
        Pageable captured = pageableCaptor.getValue();

        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(CourseController.class))
                .andExpect(handler().methodName("getAllCourses"))
                .andExpect(jsonPath("$.message").value("조회에 성공했습니다."));
        assertThat(captured.getSort()).isEqualTo(Sort.by("credit").descending());
        then(courseService).should().findAllCoursesModeFull(anyString(), anyString(), any(), any(Pageable.class));
    }
}