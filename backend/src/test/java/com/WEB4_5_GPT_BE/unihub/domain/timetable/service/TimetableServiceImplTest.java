package com.WEB4_5_GPT_BE.unihub.domain.timetable.service;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Visibility;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.TimetableCreateRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.share.TimetableShareLinkRequest;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.TimetableDetailResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.share.TimetableShareLinkResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.share.TimetableSharedViewResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.Timetable;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.share.TimetablePrivateException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.share.TimetableShareDeniedException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.share.TimetableShareLinkInvalidException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.timetable.TimetableAlreadyExistsException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.exception.timetable.TimetableNotFoundException;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.repository.TimetableItemRepository;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.repository.TimetableRepository;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableServiceImplTest {

    @Mock
    private TimetableRepository timetableRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private StringRedisTemplate redisTemplate;
    @InjectMocks
    private TimetableServiceImpl timetableService;
    @Mock
    private TimetableItemRepository timetableItemRepository;

    // 테스트에서 사용할 MockMember 추가
    private Member mockCurrentUser;

    private Member student(long memberId, String name, String email) {
        return Student.builder()
                .id(memberId)
                .name(name)
                .email(email)
                .major(Major.builder().id(1L).name("소프트웨어").build())
                .university(University.builder().id(1L).name("A대학교").build())
                .build();
    }

    @Test
    @DisplayName("시간표가 중복되면 예외를 발생시킨다")
    void givenDuplicateTimetable_whenCreateTimetable_thenThrowException() {
        // given
        Member member = student(1L, "김하늘", "student@auni.ac.kr");
        TimetableCreateRequest req = new TimetableCreateRequest(2025, 1);

        when(timetableRepository.existsByMemberIdAndYearAndSemester(1L, 2025, 1))
                .thenReturn(true);

        // when - then
        assertThatThrownBy(() -> timetableService.createTimetable(member, req))
                .isInstanceOf(TimetableAlreadyExistsException.class)
                .hasMessage("해당 학기 시간표는 이미 존재합니다.");

        verify(timetableRepository, never()).save(any(Timetable.class));
    }

    @Test
    @DisplayName("시간표가 존재하지 않으면 새 시간표를 저장한다")
    void givenValidRequest_whenCreateTimetable_thenSavesTimetable() {
        // given
        Member member = student(1L, "김하늘", "student@auni.ac.kr");
        TimetableCreateRequest req = new TimetableCreateRequest(2025, 1);

        when(timetableRepository.existsByMemberIdAndYearAndSemester(1L, 2025, 1))
                .thenReturn(false);

        // when
        timetableService.createTimetable(member, req);

        // then
        var captor = ArgumentCaptor.forClass(Timetable.class);
        verify(timetableRepository).save(captor.capture());

        Timetable saved = captor.getValue();
        assertThat(saved.getMember()).isEqualTo(member);
        assertThat(saved.getYear()).isEqualTo(2025);
        assertThat(saved.getSemester()).isEqualTo(1);
    }

    @Test
    @DisplayName("회원이 연도/학기를 기준으로 시간표를 조회하면 해당 시간표를 반환한다")
    void givenMemberAndSemester_whenGetMyTimetable_thenReturnTimetableDetailResponse() {
        // given
        Member member = student(1L, "김하늘", "student@auni.ac.kr");
        Timetable timetable = Timetable.builder()
                .id(10L).member(member).year(2025).semester(1).build();

        when(timetableRepository.findByMemberIdAndYearAndSemester(1L, 2025, 1))
                .thenReturn(java.util.Optional.of(timetable));

        // when
        TimetableDetailResponse result = timetableService.getMyTimetable(member, 2025, 1);

        // then
        assertThat(result.timetableId()).isEqualTo(10L);
        assertThat(result.year()).isEqualTo(2025);
        assertThat(result.semester()).isEqualTo(1);
    }

    @Test
    @DisplayName("해당 연도/학기의 시간표가 없으면 예외를 던진다")
    void givenInvalidSemester_whenGetMyTimetable_thenThrowException() {
        // given
        Member member = student(1L, "김하늘", "student@auni.ac.kr");

        when(timetableRepository.findByMemberIdAndYearAndSemester(1L, 2025, 1))
                .thenReturn(java.util.Optional.empty());

        // when - then
        assertThatThrownBy(() -> timetableService.getMyTimetable(member, 2025, 1))
                .isInstanceOf(TimetableNotFoundException.class)
                .hasMessage("해당 연도와 학기의 시간표가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("공유 링크 정상 생성: Redis 해시·TTL 저장 + URL/만료일 반환")
    void givenValidRequest_whenCreateShareLink_thenSaveToRedisAndReturnResponse() {
        // given
        Member owner = student(1L, "김하늘", "student@auni.ac.kr");
        Timetable tt = Timetable.builder().id(10L).member(owner).year(2025).semester(1).build();
        when(timetableRepository.findById(10L)).thenReturn(Optional.of(tt));

        // Redis 해시 Mock
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        TimetableShareLinkRequest req =
                new TimetableShareLinkRequest(10L, Visibility.PUBLIC);

        String baseUrl = "https://auni.ac.kr";

        // when
        TimetableShareLinkResponse res =
                timetableService.createShareLink(owner, req, baseUrl);

        // then ───────── Redis 저장 검증
        verify(hashOps).put(anyString(), eq("timetableId"), eq("10"));
        verify(hashOps).put(anyString(), eq("visibility"), eq("PUBLIC"));
        verify(redisTemplate).expire(anyString(), eq(Duration.ofDays(7)));

        // then ───────── 응답 필드 검증
        assertThat(res.shareUrl())
                .startsWith(baseUrl + "/timetable/share/")   // 키는 8자리 랜덤
                .hasSize(baseUrl.length() + 25);  // 대략적 길이 체크

        assertThat(res.expiresAt())
                .isCloseTo(LocalDateTime.now().plusDays(7), within(2, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("소유자가 아니면 TimetableShareDeniedException(403) 발생")
    void givenNotOwner_whenCreateShareLink_thenThrowAccessDenied() {
        // given
        Member owner    = student(1L, "김하늘", "student1@auni.ac.kr");
        Member stranger = student(2L, "김바닥", "student2@auni.ac.kr");

        Timetable tt = Timetable.builder().id(10L).member(owner).year(2025).semester(1).build();
        when(timetableRepository.findById(10L)).thenReturn(Optional.of(tt));

        TimetableShareLinkRequest req =
                new TimetableShareLinkRequest(10L, Visibility.PUBLIC);

        // when / then
        assertThatThrownBy(() -> timetableService.createShareLink(stranger, req, "http://localhost"))
                .isInstanceOf(TimetableShareDeniedException.class)
                .hasMessage("본인의 시간표만 공유할 수 있습니다.");
    }

    @Test
    @DisplayName("만료되었거나 존재하지 않는 shareKey면 TimetableShareLinkInvalidException 발생")
    void whenShareKeyMissing_thenThrowInvalidException() {
        // given
        String badKey = "nope1234";
        when(redisTemplate.hasKey(badKey)).thenReturn(false);

        mockCurrentUser = student(1L, "테스터", "tester@auni.ac.kr");

        // when / then
        assertThatThrownBy(() -> timetableService.getSharedTimetable(badKey, mockCurrentUser))
                .isInstanceOf(TimetableShareLinkInvalidException.class)
                .hasMessage("공유 링크가 만료되었거나 존재하지 않습니다.");
    }

    @Test
    @DisplayName("해시 구조가 잘못되면 TimetableShareLinkInvalidException 발생")
    void whenHashMissingFields_thenThrowInvalidException() {
        // given
        String key = "abcd1234";
        when(redisTemplate.hasKey(key)).thenReturn(true);

        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries(key)).thenReturn(Collections.emptyMap());

        mockCurrentUser = student(1L, "테스터", "tester@auni.ac.kr");

        // when / then
        assertThatThrownBy(() -> timetableService.getSharedTimetable(key, mockCurrentUser))
                .isInstanceOf(TimetableShareLinkInvalidException.class)
                .hasMessage("공유 링크가 만료되었거나 존재하지 않습니다.");
    }

    @Test
    @DisplayName("비공개 시간표를 다른 사용자가 접근하면 TimetablePrivateException 발생")
    void whenPrivateTimetableAccessedByOtherUser_thenThrowPrivateException() {
        // given
        String key = "priv0001";
        when(redisTemplate.hasKey(key)).thenReturn(true);

        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        Map<Object,Object> entries = new HashMap<>();
        entries.put("timetableId", "10");
        entries.put("visibility", "PRIVATE");
        when(hashOps.entries(key)).thenReturn(entries);

        // 시간표 소유자 설정 (ID: 999)
        Member owner = student(999L, "김하늘", "owner@auni.ac.kr");
        Timetable mockTt = mock(Timetable.class);
        when(mockTt.getMember()).thenReturn(owner);
        when(timetableRepository.findWithItemsById(10L)).thenReturn(Optional.of(mockTt));

        // 다른 사용자로 접근 시도 (ID: 1)
        mockCurrentUser = student(1L, "김다른", "other@auni.ac.kr");

        // when / then
        assertThatThrownBy(() -> timetableService.getSharedTimetable(key, mockCurrentUser))
                .isInstanceOf(TimetablePrivateException.class)
                .hasMessage("비공개 시간표는 본인만 열람할 수 있습니다.");
    }

    @Test
    @DisplayName("비공개 시간표를 본인이 접근하면 정상 조회")
    void whenPrivateTimetableAccessedByOwner_thenReturnsSharedView() {
        // given
        String key = "priv0002";
        when(redisTemplate.hasKey(key)).thenReturn(true);

        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        Map<Object,Object> entries = new HashMap<>();
        entries.put("timetableId", "10");
        entries.put("visibility", "PRIVATE");
        when(hashOps.entries(key)).thenReturn(entries);

        // 시간표 소유자 설정
        Member owner = student(999L, "김하늘", "haneul@auni.ac.kr");
        Timetable mockTt = mock(Timetable.class);
        when(mockTt.getId()).thenReturn(10L);
        when(mockTt.getYear()).thenReturn(2025);
        when(mockTt.getSemester()).thenReturn(1);
        when(mockTt.getMember()).thenReturn(owner);
        when(mockTt.getItems()).thenReturn(Collections.emptyList());

        when(timetableRepository.findWithItemsById(10L)).thenReturn(Optional.of(mockTt));
        when(timetableItemRepository.findWithSchedulesByTimetableId(10L))
                .thenReturn(Collections.emptyList());

        // 본인이 접근 (owner와 같은 ID)
        mockCurrentUser = student(999L, "김하늘", "haneul@auni.ac.kr");

        // when
        TimetableSharedViewResponse dto = timetableService.getSharedTimetable(key, mockCurrentUser);

        // then
        assertThat(dto.timetableId()).isEqualTo(10L);
        assertThat(dto.year()).isEqualTo(2025);
        assertThat(dto.semester()).isEqualTo(1);
        assertThat(dto.ownerName()).isEqualTo("김하늘");
        assertThat(dto.timetables()).isEmpty();
    }

    @Test
    @DisplayName("공개 시간표는 누구나 조회 가능")
    void whenPublicTimetable_thenAnyUserCanAccess() {
        // given
        String key = "goodkey1";
        when(redisTemplate.hasKey(key)).thenReturn(true);

        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        Map<Object,Object> entries = new HashMap<>();
        entries.put("timetableId", "10");
        entries.put("visibility", "PUBLIC");
        when(hashOps.entries(key)).thenReturn(entries);

        // 시간표 설정
        Timetable mockTt = mock(Timetable.class);
        when(mockTt.getId()).thenReturn(10L);
        when(mockTt.getYear()).thenReturn(2025);
        when(mockTt.getSemester()).thenReturn(1);
        when(mockTt.getMember()).thenReturn(student(999L, "김하늘", "haneul@auni.ac.kr"));
        when(mockTt.getItems()).thenReturn(Collections.emptyList());
        when(timetableRepository.findWithItemsById(10L)).thenReturn(Optional.of(mockTt));
        when(timetableItemRepository.findWithSchedulesByTimetableId(10L))
                .thenReturn(Collections.emptyList());

        // 다른 사용자가 접근
        mockCurrentUser = student(1L, "김다른", "other@auni.ac.kr");

        // when
        TimetableSharedViewResponse dto = timetableService.getSharedTimetable(key, mockCurrentUser);

        // then
        assertThat(dto.timetableId()).isEqualTo(10L);
        assertThat(dto.year()).isEqualTo(2025);
        assertThat(dto.semester()).isEqualTo(1);
        assertThat(dto.ownerName()).isEqualTo("김하늘");
        assertThat(dto.timetables()).isEmpty();
    }
}
