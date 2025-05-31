package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.QueueStatusDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class EnrollmentQueueServiceTest {

    private static final String WAITING_QUEUE_KEY = "enrollment:waiting-queue";
    private static final String ACTIVE_USERS_KEY = "enrollment:active-users";
    private static final String SESSION_PREFIX = "enrollment:session:";
    private static final String TEST_MEMBER_ID = "1";
    @Mock
    private StringRedisTemplate redisTemplate;
    // SSE 관련 Mock 제거
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ListOperations<String, String> listOperations;
    @InjectMocks
    private EnrollmentQueueService enrollmentQueueService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Redis 템플릿 모킹 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    @DisplayName("사용자가 즉시 접속이 허용되는 경우 테스트")
    public void testAddToQueueWithImmediateAccess() {
        // Given
        // Mock getActiveUserCount to return a low number of active users
        EnrollmentQueueService spyService = spy(enrollmentQueueService);
        doReturn(0L).when(spyService).getActiveUserCount();

        when(redisTemplate.hasKey(SESSION_PREFIX + TEST_MEMBER_ID)).thenReturn(false);
        when(listOperations.range(WAITING_QUEUE_KEY, 0, -1)).thenReturn(Collections.emptyList());

        // When
        QueueStatusDto result = spyService.addToQueue(TEST_MEMBER_ID);

        // Then
        assertTrue(result.isAllowed());
        assertEquals(0, result.getPosition());
        assertEquals(0, result.getEstimatedWaitTime());

        // 세션 설정 확인
        verify(valueOperations).set(eq(SESSION_PREFIX + TEST_MEMBER_ID), eq("active"), any(Duration.class));
    }

    @Test
    @DisplayName("사용자가 대기열에 추가되는 경우 테스트")
    public void testAddToQueueWithWaiting() {
        // Given
        // Mock getActiveUserCount to return a high number of active users (max reached)
        EnrollmentQueueService spyService = spy(enrollmentQueueService);
        doReturn(1000L).when(spyService).getActiveUserCount();

        when(redisTemplate.hasKey(SESSION_PREFIX + TEST_MEMBER_ID)).thenReturn(false);
        when(listOperations.range(WAITING_QUEUE_KEY, 0, -1)).thenReturn(Collections.emptyList());

        // When
        QueueStatusDto result = spyService.addToQueue(TEST_MEMBER_ID);

        // Then
        assertFalse(result.isAllowed());
        assertEquals(0, result.getPosition()); // 첨 번째 대기자 (0 기반)
        assertEquals(0, result.getEstimatedWaitTime()); // 실제 구현에서는 0부터 시작

        // 대기열에 추가 확인
        verify(listOperations).rightPush(WAITING_QUEUE_KEY, TEST_MEMBER_ID);
    }

    @Test
    @DisplayName("최대 접속자 100명 제한 도달 시 새 사용자가 대기열로 이동하는 테스트")
    public void testMaxConcurrentUsersHundred() {
        // 실제 EnrollmentQueueService 대신 spy 객체를 만들어 일부 메서드만 오버라이드합니다.
        EnrollmentQueueService testService = spy(enrollmentQueueService);

        // ── position 조회 시 “100번째”라고 나오도록 미리 stub
        doReturn(100).when(testService).getPositionInQueue(anyString());
        // ── 해당 사용자가 이미 대기열에 있는지 확인할 때는 false라고 응답하도록 stub
        doReturn(false).when(testService).isUserInQueue(anyString());

        // Given
        String newUserId = "999";
        // “활성 사용자 수”를 100명으로 만들어서 제한에 도달한 상태를 시뮬레이션
        doReturn(100L).when(testService).getActiveUserCount();
        // 해당 사용자가 아직 세션(= active 키)을 갖고 있지 않다고 가정
        when(redisTemplate.hasKey(SESSION_PREFIX + newUserId)).thenReturn(false);

        // When
        QueueStatusDto result = testService.addToQueue(newUserId);

        // Then
        // 1) 접속이 허용되지 않아야 함
        assertFalse(result.isAllowed(), "활성 사용자가 100명으로 제한 도달했으므로 허용되지 않아야 합니다.");

        // 2) 대기열 순서가 100번째여야 함
        assertEquals(100, result.getPosition(), "대기열 순서는 100번째여야 합니다.");

        // 3) 예상 대기시간은 0보다 커야 함 (position 100이므로)
        assertTrue(result.getEstimatedWaitTime() > 0, "대기 시간이 0보다 커야 합니다.");

        // 4) 대기열에 오른 것이 맞는지 Redis 리스트에 rightPush를 호출했는지 검증
        verify(listOperations).rightPush(WAITING_QUEUE_KEY, newUserId);
    }


    @Test
    @DisplayName("이미 활성 세션이 있는 사용자 테스트")
    public void testAddToQueueWithExistingSession() {
        // Given
        when(redisTemplate.hasKey(SESSION_PREFIX + TEST_MEMBER_ID)).thenReturn(true);

        // When
        QueueStatusDto result = enrollmentQueueService.addToQueue(TEST_MEMBER_ID);

        // Then
        assertTrue(result.isAllowed());
        assertEquals(0, result.getPosition());
        assertEquals(0, result.getEstimatedWaitTime());

        // Redis 조작이 없어야 함
        verify(valueOperations, never()).increment(ACTIVE_USERS_KEY);
        verify(valueOperations, never()).set(eq(SESSION_PREFIX + TEST_MEMBER_ID), any(), any(Duration.class));
    }

    @Test
    @DisplayName("이미 대기열에 있는 사용자 테스트")
    public void testAddToQueueWithExistingQueuePosition() {
        // Given
        when(redisTemplate.hasKey(SESSION_PREFIX + TEST_MEMBER_ID)).thenReturn(false);
        when(listOperations.range(WAITING_QUEUE_KEY, 0, -1))
                .thenReturn(Arrays.asList("2", "3", TEST_MEMBER_ID, "4"));

        // When
        QueueStatusDto result = enrollmentQueueService.addToQueue(TEST_MEMBER_ID);

        // Then
        assertFalse(result.isAllowed());
        assertEquals(3, result.getPosition()); // 세 번째 위치
        assertTrue(result.getEstimatedWaitTime() > 0);

        // 대기열에 추가하지 않아야 함
        verify(listOperations, never()).rightPush(WAITING_QUEUE_KEY, TEST_MEMBER_ID);
    }

    @Test
    @DisplayName("세션 해제 및 다음 사용자 처리 테스트")
    public void testReleaseSession() {
        // Given
        when(redisTemplate.hasKey(SESSION_PREFIX + TEST_MEMBER_ID)).thenReturn(true);
        // Mock getActiveUserCount to return a number below max concurrent users
        EnrollmentQueueService spyService = spy(enrollmentQueueService);
        doReturn(0L).when(spyService).getActiveUserCount();
        when(listOperations.leftPop(WAITING_QUEUE_KEY)).thenReturn("2"); // 다음 대기자

        // When
        spyService.releaseSession(TEST_MEMBER_ID);

        // Then
        // 세션 삭제 확인
        verify(redisTemplate).delete(SESSION_PREFIX + TEST_MEMBER_ID);

        // 다음 사용자 처리 확인
        verify(valueOperations).set(eq(SESSION_PREFIX + "2"), eq("active"), any(Duration.class));
    }

    @Test
    @DisplayName("대기열 처리 테스트")
    public void testProcessNextInQueue() {
        // Given
        EnrollmentQueueService spyService = spy(enrollmentQueueService);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        doReturn(0L).when(spyService).getActiveUserCount();
        when(listOperations.leftPop(WAITING_QUEUE_KEY)).thenReturn("5");

        // When
        spyService.processNextInQueue();

        // Then
        // 세션 활성화 확인
        verify(valueOperations).set(eq(SESSION_PREFIX + "5"), eq("active"), any(Duration.class));
    }
}