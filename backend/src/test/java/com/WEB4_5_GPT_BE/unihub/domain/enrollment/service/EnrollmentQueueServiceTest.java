package com.WEB4_5_GPT_BE.unihub.domain.enrollment.service;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.QueueStatusDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    @Mock
    private SseEmitterService sseEmitterService;
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
        when(valueOperations.increment(ACTIVE_USERS_KEY, 0)).thenReturn(500L); // 활성 사용자가 한계보다 적음
        when(redisTemplate.hasKey(SESSION_PREFIX + TEST_MEMBER_ID)).thenReturn(false);
        when(listOperations.range(WAITING_QUEUE_KEY, 0, -1)).thenReturn(Collections.emptyList());

        // When
        QueueStatusDto result = enrollmentQueueService.addToQueue(TEST_MEMBER_ID);

        // Then
        assertTrue(result.isAllowed());
        assertEquals(0, result.getPosition());
        assertEquals(0, result.getEstimatedWaitTime());

        // 세션 설정과 카운터 증가 확인
        verify(valueOperations).increment(ACTIVE_USERS_KEY);
        verify(valueOperations).set(eq(SESSION_PREFIX + TEST_MEMBER_ID), eq("active"), any(Duration.class));
        verify(sseEmitterService).sendQueueStatus(eq(TEST_MEMBER_ID), any(QueueStatusDto.class));
    }

    @Test
    @DisplayName("사용자가 대기열에 추가되는 경우 테스트")
    public void testAddToQueueWithWaiting() {
        // Given
        when(valueOperations.increment(ACTIVE_USERS_KEY, 0)).thenReturn(1000L); // 활성 사용자가 한계에 도달
        when(redisTemplate.hasKey(SESSION_PREFIX + TEST_MEMBER_ID)).thenReturn(false);
        when(listOperations.range(WAITING_QUEUE_KEY, 0, -1)).thenReturn(Collections.emptyList());

        // When
        QueueStatusDto result = enrollmentQueueService.addToQueue(TEST_MEMBER_ID);

        // Then
        assertFalse(result.isAllowed());
        assertEquals(0, result.getPosition()); // 첫 번째 대기자 (0 기반)
        assertEquals(0, result.getEstimatedWaitTime()); // 실제 구현에서는 0부터 시작

        // 대기열에 추가 확인
        verify(listOperations).rightPush(WAITING_QUEUE_KEY, TEST_MEMBER_ID);
        verify(sseEmitterService).sendQueueStatus(eq(TEST_MEMBER_ID), any(QueueStatusDto.class));
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
        when(valueOperations.increment(ACTIVE_USERS_KEY, 0)).thenReturn(999L); // 여유 공간 있음
        when(listOperations.leftPop(WAITING_QUEUE_KEY)).thenReturn("2"); // 다음 대기자

        // When
        enrollmentQueueService.releaseSession(TEST_MEMBER_ID);

        // Then
        // 세션 삭제 확인
        verify(redisTemplate).delete(SESSION_PREFIX + TEST_MEMBER_ID);
        verify(valueOperations).decrement(ACTIVE_USERS_KEY);

        // 다음 사용자 처리 확인
        verify(valueOperations).increment(ACTIVE_USERS_KEY);
        verify(valueOperations).set(eq(SESSION_PREFIX + "2"), eq("active"), any(Duration.class));
        verify(sseEmitterService).sendQueueStatus(eq("2"), argThat(QueueStatusDto::isAllowed));
    }

    @Test
    @DisplayName("대기열 상태 메시지 전송 테스트")
    public void testSendQueueStatusMessages() {
        // Given
        when(listOperations.range(WAITING_QUEUE_KEY, 0, -1))
                .thenReturn(Arrays.asList("2", "3", "4"));

        // processNextInQueue를 통해 sendQueueStatusMessages 호출 트리거
        when(valueOperations.increment(ACTIVE_USERS_KEY, 0)).thenReturn(999L);
        when(listOperations.leftPop(WAITING_QUEUE_KEY)).thenReturn("5");

        // When
        enrollmentQueueService.processNextInQueue();

        // Then
        // 메시지 전송 확인 (배치 처리 로직에 따라 다를 수 있음)
        // 실제 테스트는 내부 구현에 따라 달라질 수 있음
        ArgumentCaptor<QueueStatusDto> statusCaptor = ArgumentCaptor.forClass(QueueStatusDto.class);
        verify(sseEmitterService, atLeastOnce()).sendQueueStatus(eq("5"), statusCaptor.capture());

        QueueStatusDto capturedStatus = statusCaptor.getValue();
        assertTrue(capturedStatus.isAllowed());
    }
}
