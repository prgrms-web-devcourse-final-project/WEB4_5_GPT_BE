package com.WEB4_5_GPT_BE.unihub.global.alert;

import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AlertNotifier {

    private final RestTemplate restTemplate = new RestTemplate();
    private final StringRedisTemplate redisTemplate;

    @Value("${custom.sentry.webhook-url}")
    private String webhookUrl;

    @Value("${spring.profiles.active:unknown}")
    private String activeProfile;

    @Value("${custom.slack.duplicate-interval-ms:600000}")
    private long duplicateIntervalMs;

    /**
     * 에러 발생 시 Slack에 Block Kit 형식으로 알림 전송
     */
    public void notifyError(
            String title, Exception e, String httpMethod, String path, String params
    ) {
        if (!"prod".equalsIgnoreCase(activeProfile) && !"stg".equalsIgnoreCase(activeProfile)) {
            return;
        }
        // Sentry 전송
        Sentry.captureException(e);

        String exceptionType = e.getClass().getSimpleName();
        // 중복 알림 제어: Redis TTL 사용
        String key = "alert:exception:" + exceptionType;
        Boolean absent = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", duplicateIntervalMs, TimeUnit.MILLISECONDS);
        if (!Boolean.TRUE.equals(absent)) {
            return;
        }

        // 스택 스니펫 5줄
        String stackSnippet = Arrays.stream(e.getStackTrace())
                // 우리의 패키지만
                .filter(f -> f.getClassName().startsWith("com.WEB4_5_GPT_BE.unihub"))
                // 최대 5줄
                .limit(5)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));

        // Slack Block Kit JSON
        String payload = """
        {
          "blocks": [
            {
              "type":"header",
              "text":{"type":"plain_text","text":"🚨 [%s] %s (%s)","emoji":true}
            },
            {
              "type":"section",
              "fields":[
                {"type":"mrkdwn","text":"*메시지:*\n%3$s"},
                {"type":"mrkdwn","text":"*엔드포인트:*\n`%4$s %5$s`"},
                {"type":"mrkdwn","text":"*파라미터:*\n%6$s"}
              ]
            },
            {"type":"divider"},
            {
              "type":"section",
              "text":{"type":"mrkdwn","text":"*스택트레이스 (핵심):*\n```%7$s```"}
            }
          ]
        }
        """.formatted(
                        activeProfile.toUpperCase(),   // %1$s
                        title,                        // %2$s
                        escape(e.getMessage()),       // %3$s
                        httpMethod,                   // %4$s
                        path,                         // %5$s
                        escape(params),               // %6$s
                        escape(stackSnippet)         // %7$s
                );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        try {
            restTemplate.postForEntity(webhookUrl, request, String.class);
        } catch (Exception ex) {
            System.err.println("Slack 전송 실패: " + ex.getMessage());
        }
    }

    private String escape(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}