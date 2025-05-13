package com.WEB4_5_GPT_BE.unihub.domain.course.controller;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseRequest;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseWithFullScheduleResponse;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import com.WEB4_5_GPT_BE.unihub.domain.course.service.CourseService;
import com.WEB4_5_GPT_BE.unihub.domain.course.service.S3Service;
import com.WEB4_5_GPT_BE.unihub.domain.member.service.AuthTokenService;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.global.Rq;
import com.WEB4_5_GPT_BE.unihub.global.alert.AlertNotifier;
import com.WEB4_5_GPT_BE.unihub.global.security.CustomAuthenticationFilter;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MultipartFile;

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
    private AlertNotifier alertNotifier;

    @MockitoBean
    private Rq rq;

    @MockitoBean
    private CustomAuthenticationFilter customAuthenticationFilter;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private S3Service s3Service;

    private University testUniversity = new University(5L,
            "testUniversity", "unihub.ac.kr");

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
// 기존
//        given(courseService.createCourse(any(CourseRequest.class)))
//                .willReturn(CourseWithFullScheduleResponse.from(testCourse));
//
//        ResultActions resultActions = mockMvc.perform(post("/api/courses")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(CourseRequest.from(testCourse))));

        // 1) service stub: CourseRequest + no file
        given(courseService.createCourse(any(CourseRequest.class), isNull()))
                .willReturn(CourseWithFullScheduleResponse.from(testCourse));

        // 2) JSON part 준비 (@RequestPart("data"))
        MockMultipartFile dataPart = new MockMultipartFile(
                "data",                              // @RequestPart("data")
                "",                                  // 원본 filename
                "application/json",
                objectMapper.writeValueAsBytes(CourseRequest.from(testCourse))
        );

        // 3) multipart/form-data 요청 수행 (file 파트는 아예 넣지 않음)
        ResultActions resultActions = mockMvc.perform(
                multipart("/api/courses")
                        .file(dataPart)
                        .characterEncoding("UTF-8")    // 한글 깨짐 방지
        );

        resultActions
                // 테스트 실행시 필요한 빈만 로드되기 때문에 AOP가 동작하지 않음. 실제 응답은 정상적으로 201 Created 코드가 들어감
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(CourseController.class))
                .andExpect(handler().methodName("createCourse"))
                .andExpect(jsonPath("$.message").value("성공적으로 생성되었습니다."))
                .andExpect(jsonPath("$.data.title").value(testCourse.getTitle()))
                .andExpect(jsonPath("$.data.schedule", hasSize(2)))
                .andExpect(jsonPath("$.data.schedule[0].day").value(testCourse.getSchedules().getFirst().getDay().toString()));
        then(courseService).should()
                .createCourse(any(CourseRequest.class), isNull());
    }

    @Test
    @DisplayName("파일이 포함된 강의 정보로 생성 요청시, 업로드된 URL이 저장되어 반환된다.")
    void givenValidCourseRequestWithFile_whenCreatingCourse_thenReturnCreatedCourseWithAttachmentUrl() throws Exception {
        // — 1) 업로드된 URL 과, 그것이 세팅된 Course 객체 준비
        String uploadedUrl = "https://bucket-1.s3.ap-northeast-2.amazonaws.com/12345_plan.png";
        Course courseWithAttachment = new Course(
                testCourse.getId(),
                testCourse.getTitle(),
                testCourse.getMajor(),
                testCourse.getLocation(),
                testCourse.getCapacity(),
                testCourse.getEnrolled(),
                testCourse.getCredit(),
                testCourse.getProfessor(),
                testCourse.getGrade(),
                testCourse.getSemester(),
                uploadedUrl
        );
        courseWithAttachment.getSchedules().addAll(testCourse.getSchedules());

        // — 2) 서비스 스텁: 2-arg 버전에 스텁을 걸어줌
        given(courseService.createCourse(any(CourseRequest.class), any(MultipartFile.class)))
                .willReturn(CourseWithFullScheduleResponse.from(courseWithAttachment));

        // — 3) JSON data 파트
        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(CourseRequest.from(testCourse))
        );

        // — 4) 파일 파트
        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                "plan.png",
                "image/png",
                "dummy-png-bytes".getBytes()
        );

        mockMvc.perform(
                        multipart("/api/courses")
                                .file(dataPart)
                                .file(filePart)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .characterEncoding("UTF-8")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("성공적으로 생성되었습니다."))
                .andExpect(jsonPath("$.data.coursePlanAttachment").value(uploadedUrl))
                .andExpect(jsonPath("$.data.title").value(testCourse.getTitle()));
    }

    @Test
    @DisplayName("파일 없이 강의 수정 요청 → 성공")
    void givenValidCourseRequestWithoutFile_whenUpdatingCourse_thenReturnUpdatedCourse() throws Exception {
        Long courseId = 1L;

        given(courseService.updateCourse(
                eq(courseId),
                any(CourseRequest.class),
                isNull()
        )).willReturn(CourseWithFullScheduleResponse.from(testCourse));

        // json part 준비
        MockMultipartFile jsonPart = new MockMultipartFile(
                "data", "", "application/json",
                objectMapper.writeValueAsBytes(CourseRequest.from(testCourse))
        );

        // multipart PUT 요청 (파일 첨부 없음)
        mockMvc.perform(multipart("/api/courses/{id}", courseId)
                        .file(jsonPart)
                        .with(r -> {
                            r.setMethod("PUT");
                            return r;
                        })
                        .characterEncoding("UTF-8")
                )
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(CourseController.class))
                .andExpect(handler().methodName("updateCourse"))
                .andExpect(jsonPath("$.message").value("성공적으로 수정되었습니다."))
                .andExpect(jsonPath("$.data.title").value(testCourse.getTitle()))
                .andExpect(jsonPath("$.data.schedule", hasSize(2)))
                .andExpect(jsonPath("$.data.schedule[0].day").value(testCourse.getSchedules().getFirst().getDay().toString()));

        // verify + captor
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<CourseRequest> reqCaptor = ArgumentCaptor.forClass(CourseRequest.class);
        verify(courseService).updateCourse(
                idCaptor.capture(),
                reqCaptor.capture(),
                isNull()
        );
        assertThat(idCaptor.getValue()).isEqualTo(courseId);
    }

    @Test
    @DisplayName("강의 수정시, 파일 업로드가 정상적으로 작동하고 반환된 URL 이 그대로 response 에 담긴다")
    void givenValidRequestWithFile_whenUpdatingCourse_thenReturnCourseWithNewAttachmentUrl() throws Exception {
        Long courseId = 1L;
        String newUrl = "https://bucket-1.s3.ap-northeast-2.amazonaws.com/12345_plan.pdf";

        // 1) 원본 testCourse 와 동일한 속성 + 새로운 attachmentUrl 인스턴스 생성
        Course courseWithNewAttachment = new Course(
                testCourse.getId(),
                testCourse.getTitle(),
                testCourse.getMajor(),
                testCourse.getLocation(),
                testCourse.getCapacity(),
                testCourse.getEnrolled(),
                testCourse.getCredit(),
                testCourse.getProfessor(),
                testCourse.getGrade(),
                testCourse.getSemester(),
                newUrl
        );
        courseWithNewAttachment.getSchedules().addAll(testCourse.getSchedules());

        // 2) service stub
        given(courseService.updateCourse(
                eq(courseId),
                any(CourseRequest.class),
                any(MultipartFile.class))
        ).willReturn(CourseWithFullScheduleResponse.from(courseWithNewAttachment));

        // 3) JSON part
        MockMultipartFile json = new MockMultipartFile(
                "data", "", "application/json",
                objectMapper.writeValueAsBytes(CourseRequest.from(testCourse))
        );
        // 4) file part
        MockMultipartFile file = new MockMultipartFile(
                "file", "plan.pdf", "application/pdf", "dummy".getBytes()
        );

        // 5) multipart PUT 요청
        mockMvc.perform(multipart("/api/courses/{id}", courseId)
                        .file(json).file(file)
                        .with(r -> {
                            r.setMethod("PUT");
                            return r;
                        })
                        .characterEncoding("UTF-8")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coursePlanAttachment").value(newUrl));

        then(courseService).should()
                .updateCourse(eq(courseId), any(CourseRequest.class), any(MultipartFile.class));
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

        //SecurityUser 목 생성
        SecurityUser mockUser = mock(SecurityUser.class);

        //Spring Security 인증 컨텍스트에 설정
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(mockUser, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
        SecurityContextHolder.setContext(context);

        //Service stub
        given(courseService.findAllCoursesModeFull(
                anyString(),    // title
                anyString(),    // profName
                any(),          // majorId
                any(),          // grade
                any(),          // semester
                any(SecurityUser.class), // principal
                pageableCaptor.capture()
        )).willReturn(new PageImpl<>(List.of()));

        //요청 수행
        ResultActions resultActions = mockMvc.perform(
                get("/api/courses?mode=FULL&title=프로그래밍&profName=김교수&majorId=1&grade=2&semester=1&sort=credit,desc")
        );

        Pageable captured = pageableCaptor.getValue();

        //응답 검증
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(CourseController.class))
                .andExpect(handler().methodName("getAllCourses"))
                .andExpect(jsonPath("$.message").value("조회에 성공했습니다."));

        //메서드 호출 검증
        then(courseService).should().findAllCoursesModeFull(
                eq("프로그래밍"),
                eq("김교수"),
                eq(1L),
                eq(2),
                eq(1),
                any(SecurityUser.class),
                any(Pageable.class)
        );
    }


}