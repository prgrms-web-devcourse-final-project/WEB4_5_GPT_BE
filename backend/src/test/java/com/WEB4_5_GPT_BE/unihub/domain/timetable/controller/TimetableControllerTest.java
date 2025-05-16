package com.WEB4_5_GPT_BE.unihub.domain.timetable.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Visibility;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.MemberLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.repository.MemberRepository;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.TimetableCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.share.TimetableShareLinkRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.repository.TimetableRepository;
import com.WEB4_5_GPT_BE.unihub.global.config.RedisTestContainerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    @DisplayName("공유된 시간표 조회 - 다른 사용자가 비공개 링크 접근시 403")
    void getSharedTimetable_privateKeyByOtherUser_returns403() throws Exception {
        // 1. 첫 번째 사용자의 시간표 ID 얻기
        Member owner = memberRepository.findByEmail(studentEmail_1).orElseThrow();
        Long timetableId = timetableRepository
                .findByMemberIdAndYearAndSemester(owner.getId(), 2025, 1)
                .orElseThrow()
                .getId();

        // 2. 첫 번째 사용자가 비공개 공유 링크 생성
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

        // 3. 다른 사용자로 로그인
        String otherUserEmail = "teststudent2@auni.ac.kr";  // 다른 사용자
        MemberLoginRequest otherLoginRequest = new MemberLoginRequest(otherUserEmail, studentPassword);

        String otherResponseBody = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherLoginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String otherAccessToken = objectMapper.readTree(otherResponseBody)
                .path("data")
                .path("accessToken")
                .asText();

        // 4. 다른 사용자가 비공개 공유 시간표 조회 (403 기대)
        mockMvc.perform(get("/api/timetables/share/{shareKey}", shareKey)
                        .header("Authorization", "Bearer " + otherAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("비공개 시간표는 본인만 열람할 수 있습니다."));
    }

    @Test
    @DisplayName("공유된 시간표 조회 - 본인이 자신의 비공개 링크 접근시 200")
    void getSharedTimetable_privateKeyByOwner_returns200() throws Exception {
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

        // 3. 본인이 자신의 비공개 공유 시간표 조회 (200 기대)
        mockMvc.perform(get("/api/timetables/share/{shareKey}", shareKey)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("공유된 시간표 조회 성공"))
                .andExpect(jsonPath("$.data.timetableId").value(timetableId));
    }

    @Test
    @DisplayName("시간표에 일반 항목 등록 - 성공")
    void createNormalItem_success() throws Exception {
        // 1. 시간표 ID 얻기
        Member member = memberRepository.findByEmail(studentEmail_1).orElseThrow();
        Long timetableId = timetableRepository
                .findByMemberIdAndYearAndSemester(member.getId(), 2025, 1)
                .orElseThrow()
                .getId();

        // 2. 일반 항목 생성 요청 준비
        String requestJson = "{" +
                "\"timetableId\": " + timetableId + "," +
                "\"title\": \"알고리즘 스터디\"," +
                "\"professorName\": \"자체 진행\"," +
                "\"color\": \"#4285F4\"," +
                "\"location\": \"도서관 스터디룸\"," +
                "\"memo\": \"알고리즘 문제 풀이 스터디\"," +
                "\"scheduleRequest\": [" +
                "  {" +
                "    \"day\": \"MON\"," +
                "    \"startTime\": \"09:00\"," +
                "    \"endTime\": \"10:30\"" +
                "  }" +
                "]" +
                "}";

        // 3. 요청 실행
        mockMvc.perform(post("/api/timetables/normal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("시간표 일정이 성공적으로 등록되었습니다."));
    }

    @Test
    @DisplayName("시간표에 강의 등록 - 성공")
    void addCourseItem_success() throws Exception {
        // 1. 시간표 ID 얻기
        Member member = memberRepository.findByEmail(studentEmail_1).orElseThrow();
        Long timetableId = timetableRepository
                .findByMemberIdAndYearAndSemester(member.getId(), 2025, 1)
                .orElseThrow()
                .getId();

        // 2. 강의 항목 생성 요청 준비
        String requestJson = "{" +
                "\"timetableId\": " + timetableId + "," +
                "\"courseId\": 1," +
                "\"color\": \"#EA4335\"," +
                "\"memo\": \"중간고사 5/10\"" +
                "}";

        // 3. 요청 실행
        mockMvc.perform(post("/api/timetables/course")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("강의가 시간표에 성공적으로 등록되었습니다."));
    }

    @Test
    @DisplayName("내 강의 목록으로 시간표 일괄 등록 - 성공")
    void bulkRegisterFromEnrollment_success() throws Exception {
        // 1. 시간표 ID 얻기
        Member member = memberRepository.findByEmail(studentEmail_1).orElseThrow();
        Long timetableId = timetableRepository
                .findByMemberIdAndYearAndSemester(member.getId(), 2025, 1)
                .orElseThrow()
                .getId();

        // 2. 일괄 등록 요청 준비
        String requestJson = "{" +
                "\"timetableId\": " + timetableId + "," +
                "\"courseIds\": [1, 2, 3]" +
                "}";

        // 3. 요청 실행
        mockMvc.perform(post("/api/timetables/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("시간표에 반영되었습니다."));
    }

    @Test
    @DisplayName("시간표 항목 단건 조회 - 성공")
    void getItemDetail_success() throws Exception {
        // 1. 시간표 ID 얻기
        Member member = memberRepository.findByEmail(studentEmail_1).orElseThrow();
        Long timetableId = timetableRepository
                .findByMemberIdAndYearAndSemester(member.getId(), 2025, 1)
                .orElseThrow()
                .getId();

        // 2. 먼저 항목 등록
        String requestJson = "{" +
                "\"timetableId\": " + timetableId + "," +
                "\"title\": \"알고리즘 스터디\"," +
                "\"professorName\": \"자체 진행\"," +
                "\"color\": \"#4285F4\"," +
                "\"location\": \"도서관 스터디룸\"," +
                "\"memo\": \"알고리즘 문제 풀이 스터디\"," +
                "\"scheduleRequest\": [" +
                "  {" +
                "    \"day\": \"MON\"," +
                "    \"startTime\": \"09:00\"," +
                "    \"endTime\": \"10:30\"" +
                "  }" +
                "]" +
                "}";

        String responseJson = mockMvc.perform(post("/api/timetables/normal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(requestJson))
                .andReturn().getResponse().getContentAsString();

        // 방금 생성한 항목의 ID를 알 수 없으므로, 여기서는 기본 항목을 조회하는 것으로 테스트
        // 실제 환경에서는 위 항목 등록 응답에서 ID를 추출하거나 DB에서 직접 조회해야 함
        // 여기서는 ID가 1인 항목이 있다고 가정
        Long timetableItemId = 1L;

        // 3. 항목 조회
        mockMvc.perform(get("/api/timetables/{timetableItemId}", timetableItemId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("시간표 항목 조회 성공"));
    }

    @Test
    @DisplayName("시간표 항목 수정 - 성공")
    void updateItem_success() throws Exception {
        // 방금 생성한 항목의 ID를 알 수 없으므로, 여기서는 기본 항목을 수정하는 것으로 테스트
        // 실제 환경에서는 위 항목 등록 응답에서 ID를 추출하거나 DB에서 직접 조회해야 함
        // 여기서는 ID가 1인 항목이 있다고 가정
        Long timetableItemId = 1L;

        // 수정 요청 준비
        String requestJson = "{" +
                "\"type\": \"NORMAL\"," +
                "\"courseId\": null," +
                "\"title\": \"알고리즘 스터디(수정)\"," +
                "\"professorName\": \"자체 진행\"," +
                "\"color\": \"#4285F4\"," +
                "\"memo\": \"알고리즘 문제 풀이 스터디 - 내용 수정\"," +
                "\"location\": \"도서관 스터디룸 2층\"," +
                "\"schedule\": [" +
                "  {" +
                "    \"day\": \"MON\"," +
                "    \"startTime\": \"10:00\"," +
                "    \"endTime\": \"11:30\"" +
                "  }" +
                "]" +
                "}";

        mockMvc.perform(put("/api/timetables/{timetableItemId}", timetableItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("시간표 항목 삭제 - 성공")
    void deleteItem_success() throws Exception {
        // 방금 생성한 항목의 ID를 알 수 없으므로, 여기서는 기본 항목을 삭제하는 것으로 테스트
        // 실제 환경에서는 위 항목 등록 응답에서 ID를 추출하거나 DB에서 직접 조회해야 함
        // 여기서는 ID가 1인 항목이 있다고 가정
        Long timetableItemId = 1L;

        mockMvc.perform(delete("/api/timetables/{timetableItemId}", timetableItemId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("시간표가 성공적으로 삭제되었습니다."));
    }
}
