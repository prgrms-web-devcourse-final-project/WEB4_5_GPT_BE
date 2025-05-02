package com.WEB4_5_GPT_BE.unihub.domain.enrollment.controller;

import com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.QueueStatusDto;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.EnrollmentQueueService;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.service.SseEmitterService;
import com.WEB4_5_GPT_BE.unihub.global.security.SecurityUser;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class EnrollmentEventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SseEmitterService sseEmitterService;

    @Mock
    private EnrollmentQueueService enrollmentQueueService;

    @Mock
    private SecurityUser securityUser;

    @InjectMocks
    private EnrollmentEventController enrollmentEventController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        // SecurityUser mock 설정
        when(securityUser.getId()).thenReturn(1L);

        // SseEmitter 서비스 설정
        SseEmitter mockEmitter = mock(SseEmitter.class);
        when(sseEmitterService.createEmitter(anyString())).thenReturn(mockEmitter);

        // EnrollmentQueueService 설정
        QueueStatusDto mockStatus = new QueueStatusDto(true, 5, 10, "테스트 메시지");
        when(enrollmentQueueService.addToQueue(anyString())).thenReturn(mockStatus);
        when(enrollmentQueueService.getQueueStatus(anyString())).thenReturn(mockStatus);
        when(enrollmentQueueService.isUserInQueue(anyString())).thenReturn(true);
        when(enrollmentQueueService.getPositionInQueue(anyString())).thenReturn(5);

        // MockMvc 설정 with SecurityUser 리졸버
        mockMvc = MockMvcBuilders.standaloneSetup(enrollmentEventController)
                .setCustomArgumentResolvers(new SecurityUserArgumentResolver(securityUser))
                .build();
    }

    @Test
    @DisplayName("SSE 연결 요청 테스트")
    public void testSubscribeToEvents() throws Exception {
        mockMvc.perform(get("/api/enrollment/events")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(sseEmitterService, times(1)).createEmitter("1");
    }

    @Test
    @DisplayName("대기열 추가 요청 테스트")
    public void testJoinQueue() throws Exception {
        mockMvc.perform(post("/api/enrollment/queue/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("대기열 참여 요청 성공"))
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.position").value(5))
                .andExpect(jsonPath("$.data.estimatedWaitTime").value(10))
                .andExpect(jsonPath("$.data.message").exists());

        verify(enrollmentQueueService, times(1)).addToQueue("1");
    }

    @Test
    @DisplayName("대기열 상태 조회 요청 테스트")
    public void testGetQueueStatus() throws Exception {
        mockMvc.perform(get("/api/enrollment/queue/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("대기열 상태 조회 성공"))
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.position").value(5))
                .andExpect(jsonPath("$.data.estimatedWaitTime").value(10))
                .andExpect(jsonPath("$.data.message").exists());

        verify(enrollmentQueueService, times(1)).getQueueStatus("1");
    }

    @Test
    @DisplayName("세션 해제 요청 테스트")
    public void testReleaseSession() throws Exception {
        mockMvc.perform(post("/api/enrollment/queue/release")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("세션이 정상적으로 종료되었습니다."));

        verify(enrollmentQueueService, times(1)).releaseSession("1");
    }

    /**
     * SecurityUser 파라미터를 자동으로 해결해주는 리졸버 구현
     */
    static class SecurityUserArgumentResolver implements HandlerMethodArgumentResolver {
        private final SecurityUser securityUser;

        public SecurityUserArgumentResolver(SecurityUser securityUser) {
            this.securityUser = securityUser;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(SecurityUser.class);
        }

        @Override
        public @NotNull Object resolveArgument(@NotNull MethodParameter parameter, @NotNull ModelAndViewContainer mavContainer,
                                               @NotNull NativeWebRequest webRequest, @NotNull WebDataBinderFactory binderFactory) {
            return securityUser;
        }
    }
}
