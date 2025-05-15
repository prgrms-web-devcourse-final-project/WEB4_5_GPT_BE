package com.WEB4_5_GPT_BE.unihub.domain.timetable.controller;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Visibility;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.MemberLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.TimetableCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.share.TimetableShareLinkRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.repository.TimetableRepository;
import com.WEB4_5_GPT_BE.unihub.global.config.RedisTestContainerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RedisTestContainerConfig
class TimetableControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TimetableRepository timetableRepository;

    private String accessToken;

    private static final String studentEmail_1 = "teststudent@auni.ac.kr";
    private static final String studentPassword = "password";

    @BeforeEach
    void setUp() throws Exception {
        // given: 로그인할 사용자 정보
        MemberLoginRequest loginRequest = new MemberLoginRequest(studentEmail_1, studentPassword);

        // when: 로그인 요청
        String responseBody = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then: accessToken 추출
        accessToken = objectMapper.readTree(responseBody)
                .path("data")
                .path("accessToken")
                .asText();
    }

    @Test
    @DisplayName("시간표 생성 - 성공")
    void createTimetable_success() throws Exception {
        int year = 2024;
        int semester = 1;

        mockMvc.perform(post("/api/timetables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(new TimetableCreateRequest(year, semester))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("시간표가 성공적으로 생성되었습니다."));
    }

    @Test
    @DisplayName("시간표 조회 - 성공")
    void getTimetable_success() throws Exception {
        int year = 2025;
        int semester = 1;

        // 조회
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/timetables/me")
                        .param("year", String.valueOf(year))
                        .param("semester", String.valueOf(semester))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.year").value(year))
                .andExpect(jsonPath("$.data.semester").value(semester));
    }

    @Test
    @DisplayName("공유 링크 생성 - 성공")
    void createShareLink_success() throws Exception {
        Member member = memberRepository.findByEmail(studentEmail_1)
                .orElseThrow();

        Long timetableId = timetableRepository
                .findByMemberIdAndYearAndSemester(member.getId(), 2025, 1)
                .orElseThrow()
                .getId();

        // 공유 링크 요청 바디
        TimetableShareLinkRequest req = new TimetableShareLinkRequest(timetableId, Visibility.PUBLIC);

        // ─── [2] 공유 링크 생성 요청 & 검증 ────────────────────────────
        mockMvc.perform(post("/api/timetables/share/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Client-Base-Url", "https://auni.ac.kr")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.data.shareUrl").value(
                        org.hamcrest.Matchers.startsWith("https://auni.ac.kr/timetable/share/")))
                .andExpect(jsonPath("$.data.expiresAt").exists());
    }

    @Test
    @DisplayName("공유 링크 생성 - 헤더 누락 시 400 반환")
    void createShareLink_missingHeader_returns400() throws Exception {
        // given: (timetableId = 1, visibility = PUBLIC) 가정
        TimetableShareLinkRequest req = new TimetableShareLinkRequest(1L, Visibility.PUBLIC);

        mockMvc.perform(post("/api/timetables/share/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("X-Client-Base-Url 헤더가 필요합니다."));
    }

    @Test
    @DisplayName("공유된 시간표 조회 - 성공")
    void getSharedTimetable_success() throws Exception {
        // 1. 시간표 ID 얻기
        Member member = memberRepository.findByEmail(studentEmail_1).orElseThrow();
        Long timetableId = timetableRepository
                .findByMemberIdAndYearAndSemester(member.getId(), 2025, 1)
                .orElseThrow()
                .getId();

        // 2. 공유 링크 생성 (공개)
        TimetableShareLinkRequest req = new TimetableShareLinkRequest(timetableId, Visibility.PUBLIC);
        String responseJson = mockMvc.perform(post("/api/timetables/share/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Client-Base-Url", "https://auni.ac.kr")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.data.shareUrl").exists())
                .andReturn().getResponse().getContentAsString();

        String shareUrl = objectMapper.readTree(responseJson).path("data").path("shareUrl").asText();
        String shareKey = shareUrl.substring(shareUrl.lastIndexOf('/') + 1);

        // 3. 공유된 시간표 조회
        mockMvc.perform(get("/api/timetables/share/{shareKey}", shareKey)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("공유된 시간표 조회 성공"))
                .andExpect(jsonPath("$.data.timetableId").value(timetableId));
    }

    @Test
    @DisplayName("공유된 시간표 조회 - 만료/존재하지 않는 shareKey 404")
    void getSharedTimetable_invalidKey_returns404() throws Exception {
        String fakeShareKey = "notreal12";
        mockMvc.perform(get("/api/timetables/share/{shareKey}", fakeShareKey)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message", containsString("만료되었거나 존재하지 않습니다")));
    }

    @Test
    @DisplayName("공유된 시간표 조회 - 비공개 링크 403")
    void getSharedTimetable_privateKey_returns403() throws Exception {
        // 1. 시간표 ID 얻기
        Member member = memberRepository.findByEmail(studentEmail_1).orElseThrow();
        Long timetableId = timetableRepository
                .findByMemberIdAndYearAndSemester(member.getId(), 2025, 1)
                .orElseThrow()
                .getId();

        // 2. 공유 링크 생성 (비공개)
        TimetableShareLinkRequest req = new TimetableShareLinkRequest(timetableId, Visibility.PRIVATE);
        String responseJson = mockMvc.perform(post("/api/timetables/share/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Client-Base-Url", "https://auni.ac.kr")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String shareUrl = objectMapper.readTree(responseJson).path("data").path("shareUrl").asText();
        String shareKey = shareUrl.substring(shareUrl.lastIndexOf('/') + 1);

        // 3. 비공개 공유 시간표 조회 (403 기대)
        mockMvc.perform(get("/api/timetables/share/{shareKey}", shareKey)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("해당 시간표는 비공개입니다."));
    }
}
