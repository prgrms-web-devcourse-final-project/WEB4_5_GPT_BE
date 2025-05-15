package com.WEB4_5_GPT_BE.unihub.domain.enrollment.controller;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.request.EnrollmentRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.MemberLoginRequest;
import com.WEB4_5_GPT_BE.unihub.global.config.RedisTestContainerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@DisplayName("수강 신청 관련 API 테스트")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RedisTestContainerConfig
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CourseRepository courseRepository;

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        MemberLoginRequest request = new MemberLoginRequest(email, password);

        String response = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("내 수강목록 조회 - 성공")
    void getMyEnrollmentList_success() throws Exception {

        // 학생 로그인 후 accessToken을 발급받습니다.
        String accessToken = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");

        mockMvc.perform(get("/api/enrollments/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("내 수강목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("내 수강목록 조회 – 실패 (토큰 누락)")
    void getMyEnrollmentList_throwsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/enrollments/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    @Transactional
    @DisplayName("수강 신청 - 성공")
    void enrollment_success() throws Exception {
        // given: 학생 로그인 후 accessToken을 발급받고, 기존 수강 신청 내역이 2개임을 확인
        String accessToken = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");

        mockMvc.perform(get("/api/enrollments/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // when: 네트워크 강좌를 수강 신청 요청
        Course course = courseRepository.findAll().stream()
                .filter(c -> "네트워크".equals(c.getTitle()))
                .findFirst().get();

        Integer availableSeats = course.getAvailableSeats();
        Long courseId = course.getId();

        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentRequest(courseId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("수강 신청이 완료되었습니다."));

        // then: 내 수강목록 조회 시 신청 내역이 3개로 증가해야 함
        // then: 신청 강좌의 신청 가능 인원이 1 감소해야 함
        mockMvc.perform(get("/api/enrollments/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.courseTitle=='네트워크')].availableSeats")
                        .value(availableSeats - 1));
    }

    @Test
    @DisplayName("수강 신청 실패 – 강좌 정보가 없는 경우")
    void enrollment_fail_courseNotFound() throws Exception {
        // given: 로그인하여 accessToken 획득
        String accessToken = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");
        Long invalidCourseId = 9_999L;

        // when: 존재하지 않는 강좌 ID로 수강신청을 요청하면
        // then: 400 Bad Request, 코드·메시지 확인
        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentRequest(invalidCourseId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("해당 강의가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("수강 신청 실패 – 수강신청 기간 정보가 없는 경우")
    void enrollment_fail_noPeriod() throws Exception {
        // given: 2학년학생으로 로그인 (테스트데이터에는 2학년 수강신청 기간이 없음)
        String accessToken2 = loginAndGetAccessToken("teststudent3@auni.ac.kr", "password");

        Long courseId = courseRepository.findAll().stream()
                .filter(c -> "네트워크".equals(c.getTitle()))
                .findFirst().get().getId();

        // when: 기간 설정이 없는 학년으로 수강신청 요청
        // then: 400 Bad Request, 코드·메시지 확인
        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + accessToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentRequest(courseId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("수강신청 기간 정보가 없습니다."));
    }

    @Test
    @DisplayName("수강 신청 실패 – 정원 초과")
    void enrollment_fail_capacityExceeded() throws Exception {
        // given: 로그인, '정원초과강좌' ID
        String accessToken = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");

        Long fullCourseId = courseRepository.findAll().stream()
                .filter(c -> "정원초과강좌".equals(c.getTitle()))
                .findFirst().get().getId();

        // when: 정원초과강좌로 수강신청 요청
        // then: 400 Bad Request, 코드·메시지 확인
        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentRequest(fullCourseId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409"))
                .andExpect(jsonPath("$.message").value("정원이 초과되어 수강 신청이 불가능합니다."));
    }

    @Test
    @DisplayName("수강 신청 실패 – 동일 강좌 중복 신청 시")
    void enrollment_fail_duplicate() throws Exception {
        // given: 로그인, 이미 신청된 '자료구조' 과목 ID
        String accessToken = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");

        Long dsCourseId = courseRepository.findAll().stream()
                .filter(c -> "자료구조".equals(c.getTitle()))
                .findFirst().get().getId();

        // when: 동일 과목으로 재신청 요청
        // then: 400 Bad Request, 코드·메시지 확인
        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentRequest(dsCourseId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409"))
                .andExpect(jsonPath("$.message").value("이미 신청한 강의입니다."));
    }

    @Test
    @Transactional
    @DisplayName("수강 신청 실패 – 최대 학점 초과 시")
    void enrollment_fail_creditLimit() throws Exception {
        // given: 로그인, '학점초과강좌' ID
        String accessToken = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");

        Long heavyCourseId = courseRepository.findAll().stream()
                .filter(c -> "학점초과강좌".equals(c.getTitle()))
                .findFirst().get().getId();

        // when: 총합(20 + 기존 3) > 21 학점으로 수강신청 요청
        // then: 400 Bad Request, 코드·메시지 확인
        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentRequest(heavyCourseId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409"))
                .andExpect(jsonPath("$.message").value("학점 한도를 초과하여 수강신청할 수 없습니다."));
    }

    @Test
    @Transactional
    @DisplayName("수강 신청 실패 – 시간표 충돌 시")
    void enrollment_fail_scheduleConflict() throws Exception {
        // given: 로그인, '충돌강좌' ID
        String accessToken = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");

        Long conflictCourseId = courseRepository.findAll().stream()
                .filter(c -> "충돌강좌".equals(c.getTitle()))
                .findFirst().get().getId();

        // when: 시간표가 겹치는 강좌로 수강신청 요청
        // then: 400 Bad Request, 코드·메시지 확인
        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentRequest(conflictCourseId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409"))
                .andExpect(jsonPath("$.message").value("기존 신청한 강의와 시간이 겹칩니다."));
    }

    @Test
    @DisplayName("수강 취소 - 성공")
    @Transactional
    void cancelEnrollment_success() throws Exception {
        // given: 학생 로그인 후 accessToken 발급 및 초기 수강신청 내역(2개) 확인
        String accessToken = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");

        mockMvc.perform(get("/api/enrollments/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 취소할 강좌 ID 조회
        Course course = courseRepository.findAll().stream()
                .filter(c -> "자료구조".equals(c.getTitle()))
                .findFirst()
                .get();

        Integer enrolled = course.getEnrolled();
        Long courseId = course.getId();

        // when: 해당 강좌 취소 요청
        mockMvc.perform(delete("/api/enrollments/{courseId}", courseId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("수강 취소가 완료되었습니다."));

        // then: 수강신청 목록이 1개로 감소했는지 검증
        mockMvc.perform(get("/api/enrollments/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // then: 취소 강좌의 현재 수강 인원이 1명 감소했는지 검증
        mockMvc.perform(get("/api/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrolled").value(enrolled - 1));
    }

    @Test
    @DisplayName("수강 취소 실패 – 수강신청 기간 정보 없음")
    void cancelMyEnrollment_throws1() throws Exception {
        // given: 2학년학생으로 로그인 (테스트데이터에는 2학년 수강신청 기간이 없음)
        String accessToken = loginAndGetAccessToken("teststudent3@auni.ac.kr", "password");

        Long courseId = courseRepository.findAll().stream()
                .filter(c -> "네트워크".equals(c.getTitle()))
                .findFirst().get().getId();

        // when & then: 400 Bad Request, “수강신청 기간 정보가 없습니다.”
        mockMvc.perform(delete("/api/enrollments/{courseId}", courseId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("수강신청 기간 정보가 없습니다."));
    }

    @Test
    @DisplayName("수강 취소 실패 – 수강신청 내역이 없는 경우")
    void cancelMyEnrollment_throws2() throws Exception {
        // given: 학생 로그인 후 토큰 발급
        String accessToken = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");
        Long courseId = 999L;

        // when & then: 400 Bad Request, “수강신청 내역을 찾을 수 없습니다.”
        mockMvc.perform(delete("/api/enrollments/{courseId}", courseId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("수강신청 내역이 존재하지 않습니다."));
    }

    @Test
    @DisplayName("내 수강신청 기간 조회 - 성공")
    @Transactional
    void getEnrollmentPeriod_success() throws Exception {

        // given: 학생 로그인 후 accessToken 발급
        String accessToken = loginAndGetAccessToken("teststudent@auni.ac.kr", "password");

        // when / then
        mockMvc.perform(get("/api/enrollments/periods/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("내 수강신청 기간 정보를 조회했습니다."))
                // data 필드 존재 및 타입 검증
                .andExpect(jsonPath("$.data.startDate").exists())
                .andExpect(jsonPath("$.data.endDate").exists())
                .andExpect(jsonPath("$.data.isEnrollmentOpen").value(true));
    }

    @Test
    @DisplayName("내 수강신청 기간 조회 - 정보 없음")
    @Transactional
    void getEnrollmentPeriod_noInfo() throws Exception {

        // given: 학생 로그인 후 accessToken 발급
        String accessToken = loginAndGetAccessToken("teststudent3@auni.ac.kr", "password");

        // when / then
        mockMvc.perform(get("/api/enrollments/periods/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("내 수강신청 기간 정보를 조회했습니다."))
                // data 필드 존재 및 타입 검증
                .andExpect(jsonPath("$.data.startDate").doesNotExist())
                .andExpect(jsonPath("$.data.endDate").doesNotExist())
                .andExpect(jsonPath("$.data.isEnrollmentOpen").value(false));
    }

    @Test
    @DisplayName("내 수강신청 기간 조회 - 이미 지난 기간")
    @Transactional
    void getEnrollmentPeriod_expired() throws Exception {

        // given: 학생 로그인 후 accessToken 발급
        String accessToken = loginAndGetAccessToken("test3rdstudent@auni.ac.kr", "password");

        // when / then
        mockMvc.perform(get("/api/enrollments/periods/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("내 수강신청 기간 정보를 조회했습니다."))
                // data 필드 존재 및 타입 검증
                .andExpect(jsonPath("$.data.startDate").exists())
                .andExpect(jsonPath("$.data.endDate").exists())
                .andExpect(jsonPath("$.data.isEnrollmentOpen").value(false));
    }

    @Test
    @DisplayName("내 수강신청 기간 조회 - 기간 아직 안됨")
    @Transactional
    void getEnrollmentPeriod_notYet() throws Exception {

        // given: 학생 로그인 후 accessToken 발급
        String accessToken = loginAndGetAccessToken("test3rdstudent@auni.ac.kr", "password");

        // when / then
        mockMvc.perform(get("/api/enrollments/periods/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("내 수강신청 기간 정보를 조회했습니다."))
                // data 필드 존재 및 타입 검증
                .andExpect(jsonPath("$.data.startDate").exists())
                .andExpect(jsonPath("$.data.endDate").exists())
                .andExpect(jsonPath("$.data.isEnrollmentOpen").value(false));
    }

}