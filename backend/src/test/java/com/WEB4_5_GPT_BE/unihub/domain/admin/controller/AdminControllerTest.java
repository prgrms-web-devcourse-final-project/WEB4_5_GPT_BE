package com.WEB4_5_GPT_BE.unihub.domain.admin.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.AdminInviteRequest;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.EnrollmentPeriodRequest;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.EnrollmentPeriodSearchRequest;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.ProfessorApprovalRequest;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.ProfessorSearchRequest;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.request.StudentSearchRequest;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.EnrollmentPeriodResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.ProfessorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response.StudentResponse;
import com.WEB4_5_GPT_BE.unihub.domain.admin.service.AdminService;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.request.MajorRequest;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.request.UniversityRequest;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.MajorResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.dto.response.UniversityResponse;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.MajorService;
import com.WEB4_5_GPT_BE.unihub.domain.university.service.UniversityService;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("관리자 도메인 컨트롤러 레이어 테스트")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    private AdminService adminService;

    private UniversityService universityService;

    private MajorService majorService;

    @BeforeEach
    void setUp() {
        // 서비스 목 초기화
        adminService = mock(AdminService.class);
        universityService = mock(UniversityService.class);
        majorService = mock(MajorService.class);

        // AdminController에 목 서비스 주입
        ReflectionTestUtils.setField(
                applicationContext.getBean(AdminController.class),
                "adminService",
                adminService
        );

        ReflectionTestUtils.setField(
                applicationContext.getBean(AdminController.class),
                "universityService",
                universityService
        );

        ReflectionTestUtils.setField(
                applicationContext.getBean(AdminController.class),
                "majorService",
                majorService
        );

        // 관리자 권한 설정
        SecurityUser mockUser = mock(SecurityUser.class);
        given(mockUser.getUsername()).willReturn("admin@unihub.com");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                mockUser, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        SecurityContextHolder.setContext(context);
    }


    @Test
    @DisplayName("학생 목록 조회 시 성공")
    void givenSearchParams_whenRequestingStudents_thenReturnStudentList() throws Exception {
        // given
        Page<StudentResponse> studentPage = new PageImpl<>(List.of(
                new StudentResponse(1L, "테스트대학", "홍길동", "2023001", "컴퓨터공학과", 3, 1, null)
        ));

        given(adminService.getStudents(any(StudentSearchRequest.class), any(Pageable.class)))
                .willReturn(studentPage);

        // when
        ResultActions result = mockMvc.perform(get("/api/admin/students")
                .param("universityId", "1")
                .param("majorId", "2")
                .param("grade", "3")
                .param("semester", "1"));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].memberName").value("홍길동"))
                .andExpect(jsonPath("$.data.content[0].studentCode").value("2023001"));

        then(adminService).should().getStudents(any(StudentSearchRequest.class), any(Pageable.class));
    }

    @Test
    @DisplayName("교수 목록 조회 시 성공")
    void givenSearchParams_whenRequestingProfessors_thenReturnProfessorList() throws Exception {
        // given
        Page<ProfessorResponse> professorPage = new PageImpl<>(List.of(
                new ProfessorResponse(1L, "테스트대학", "김교수", "컴퓨터공학과", ApprovalStatus.PENDING, null)
        ));

        given(adminService.getProfessors(any(ProfessorSearchRequest.class), any(Pageable.class)))
                .willReturn(professorPage);

        // when
        ResultActions result = mockMvc.perform(get("/api/admin/professors")
                .param("universityId", "1")
                .param("professorName", "김교수")
                .param("majorId", "2")
                .param("status", "PENDING"));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].memberName").value("김교수"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));

        then(adminService).should().getProfessors(any(ProfessorSearchRequest.class), any(Pageable.class));
    }

    @Test
    @DisplayName("교수 상태 변경 시 성공")
    void givenProfessorIdAndStatus_whenChangingStatus_thenReturnSuccess() throws Exception {
        // given
        Long professorId = 1L;
        ProfessorApprovalRequest request = new ProfessorApprovalRequest(ApprovalStatus.APPROVED);

        doNothing().when(adminService).changeProfessorStatus(anyLong(), any(ProfessorApprovalRequest.class));

        // when
        ResultActions result = mockMvc.perform(patch("/api/admin/professors/{memberId}", professorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result
                .andExpect(status().isOk());

        then(adminService).should().changeProfessorStatus(eq(professorId), any(ProfessorApprovalRequest.class));
    }

    @Test
    @DisplayName("수강신청 기간 조회 시 성공")
    void givenSearchParams_whenRequestingEnrollmentPeriods_thenReturnEnrollmentPeriodList() throws Exception {
        // given
        Page<EnrollmentPeriodResponse> periods = new PageImpl<>(List.of(
                new EnrollmentPeriodResponse(1L, "테스트대학", 2023, 1, 1,
                        LocalDate.of(2023, 1, 15), LocalDate.of(2023, 1, 22))
        ));

        given(adminService.getEnrollmentPeriods(any(EnrollmentPeriodSearchRequest.class), any(Pageable.class)))
                .willReturn(periods);

        // when
        ResultActions result = mockMvc.perform(get("/api/admin/enrollment-periods")
                .param("universityName", "테스트대학")
                .param("startDateFrom", "2023-01-01")
                .param("startDateTo", "2023-01-31")
                .param("endDateFrom", "2023-02-01")
                .param("endDateTo", "2023-02-28"));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].universityName").value("테스트대학"))
                .andExpect(jsonPath("$.data.content[0].year").value(2023));

        then(adminService).should().getEnrollmentPeriods(any(EnrollmentPeriodSearchRequest.class), any(Pageable.class));
    }

    @Test
    @DisplayName("수강신청 기간 생성 시 성공")
    void givenEnrollmentPeriodRequest_whenCreatingPeriod_thenReturnCreatedPeriod() throws Exception {
        // given
        EnrollmentPeriodRequest request = new EnrollmentPeriodRequest(
                1L, 2023, 1, 1, "2023-03-01", "2023-03-07"
        );

        EnrollmentPeriodResponse response = new EnrollmentPeriodResponse(
                1L, "테스트대학", 2023, 1, 1,
                LocalDate.of(2023, 3, 1), LocalDate.of(2023, 3, 7)
        );

        given(adminService.createEnrollmentPeriod(any(EnrollmentPeriodRequest.class)))
                .willReturn(response);

        // when
        ResultActions result = mockMvc.perform(post("/api/admin/enrollment-periods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.universityName").value("테스트대학"))
                .andExpect(jsonPath("$.data.year").value(2023));

        then(adminService).should().createEnrollmentPeriod(any(EnrollmentPeriodRequest.class));
    }

    @Test
    @DisplayName("수강신청 기간 수정 시 성공")
    void givenPeriodIdAndRequest_whenUpdatingPeriod_thenReturnUpdatedPeriod() throws Exception {
        // given
        Long periodId = 1L;
        EnrollmentPeriodRequest request = new EnrollmentPeriodRequest(
                1L, 2023, 1, 2, "2023-04-01", "2023-04-07"
        );

        EnrollmentPeriodResponse response = new EnrollmentPeriodResponse(
                1L, "테스트대학", 2023, 1, 2,
                LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 7)
        );

        given(adminService.updateEnrollmentPeriod(anyLong(), any(EnrollmentPeriodRequest.class)))
                .willReturn(response);

        // when
        ResultActions result = mockMvc.perform(put("/api/admin/enrollment-periods/{periodId}", periodId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.universityName").value("테스트대학"))
                .andExpect(jsonPath("$.data.semester").value(2));

        then(adminService).should().updateEnrollmentPeriod(eq(periodId), any(EnrollmentPeriodRequest.class));
    }

    @Test
    @DisplayName("수강신청 기간 삭제 시 성공")
    void givenPeriodId_whenDeletingPeriod_thenReturnSuccess() throws Exception {
        // given
        Long periodId = 1L;
        doNothing().when(adminService).deleteEnrollmentPeriod(anyLong());

        // when
        ResultActions result = mockMvc.perform(delete("/api/admin/enrollment-periods/{periodId}", periodId));

        // then
        result
                .andExpect(status().isOk());

        then(adminService).should().deleteEnrollmentPeriod(eq(periodId));
    }

    @Test
    @DisplayName("관리자 초대 시 성공")
    void givenAdminInviteRequest_whenInvitingAdmin_thenReturnSuccess() throws Exception {
        // given
        AdminInviteRequest request = new AdminInviteRequest("admin@unihub.com", "관리자");
        doNothing().when(adminService).inviteAdmin(any(AdminInviteRequest.class));

        // when
        ResultActions result = mockMvc.perform(post("/api/admin/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result
                .andExpect(status().isOk());

        then(adminService).should().inviteAdmin(any(AdminInviteRequest.class));
    }

    @Test
    @DisplayName("대학 생성 시 성공")
    void givenUniversityRequest_whenCreatingUniversity_thenReturnCreatedUniversity() throws Exception {
        // given
        UniversityRequest request = new UniversityRequest("테스트대학교", "test.ac.kr");
        UniversityResponse response = new UniversityResponse(1L, "테스트대학교", "test.ac.kr");

        given(universityService.createUniversity(any(UniversityRequest.class)))
                .willReturn(response);

        // when
        ResultActions result = mockMvc.perform(post("/api/admin/universities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("테스트대학교"))
                .andExpect(jsonPath("$.data.emailDomain").value("test.ac.kr"));

        then(universityService).should().createUniversity(any(UniversityRequest.class));
    }

    @Test
    @DisplayName("대학 정보 수정 시 성공")
    void givenUniversityIdAndRequest_whenUpdatingUniversity_thenReturnUpdatedUniversity() throws Exception {
        // given
        Long universityId = 1L;
        UniversityRequest request = new UniversityRequest("수정된대학교", "updated.ac.kr");
        UniversityResponse response = new UniversityResponse(1L, "수정된대학교", "updated.ac.kr");

        given(universityService.updateUniversity(anyLong(), any(UniversityRequest.class)))
                .willReturn(response);

        // when
        ResultActions result = mockMvc.perform(put("/api/admin/universities/{universityId}", universityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정된대학교"))
                .andExpect(jsonPath("$.data.emailDomain").value("updated.ac.kr"));

        then(universityService).should().updateUniversity(eq(universityId), any(UniversityRequest.class));
    }

    @Test
    @DisplayName("대학 삭제 시 성공")
    void givenUniversityId_whenDeletingUniversity_thenReturnSuccess() throws Exception {
        // given
        Long universityId = 1L;
        doNothing().when(universityService).deleteUniversity(anyLong());

        // when
        ResultActions result = mockMvc.perform(delete("/api/admin/universities/{universityId}", universityId));

        // then
        result
                .andExpect(status().isOk());

        then(universityService).should().deleteUniversity(eq(universityId));
    }

    @Test
    @DisplayName("전공 생성 시 성공")
    void givenMajorRequest_whenCreatingMajor_thenReturnCreatedMajor() throws Exception {
        // given
        MajorRequest request = new MajorRequest(1L, "컴퓨터공학과");
        MajorResponse response = new MajorResponse(1L, "컴퓨터공학과");

        given(majorService.createMajor(any(MajorRequest.class)))
                .willReturn(response);

        // when
        ResultActions result = mockMvc.perform(post("/api/admin/majors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("컴퓨터공학과"));

        then(majorService).should().createMajor(any(MajorRequest.class));
    }

    @Test
    @DisplayName("전공 정보 수정 시 성공")
    void givenMajorIdAndRequest_whenUpdatingMajor_thenReturnUpdatedMajor() throws Exception {
        // given
        Long majorId = 1L;
        MajorRequest request = new MajorRequest(1L, "수정된전공");
        MajorResponse response = new MajorResponse(1L, "수정된전공");

        given(majorService.updateMajor(anyLong(), any(MajorRequest.class)))
                .willReturn(response);

        // when
        ResultActions result = mockMvc.perform(put("/api/admin/majors/{majorId}", majorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정된전공"));

        then(majorService).should().updateMajor(eq(majorId), any(MajorRequest.class));
    }

    @Test
    @DisplayName("전공 삭제 시 성공")
    void givenMajorId_whenDeletingMajor_thenReturnSuccess() throws Exception {
        // given
        Long majorId = 1L;
        doNothing().when(majorService).deleteMajor(anyLong());

        // when
        ResultActions result = mockMvc.perform(delete("/api/admin/majors/{majorId}", majorId));

        // then
        result
                .andExpect(status().isOk());

        then(majorService).should().deleteMajor(eq(majorId));
    }
}
