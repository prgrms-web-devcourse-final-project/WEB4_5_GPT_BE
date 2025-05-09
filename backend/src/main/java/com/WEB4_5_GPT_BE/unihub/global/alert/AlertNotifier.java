package com.WEB4_5_GPT_BE.unihub.global.alert;

import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AlertNotifier {

    @Value("${custom.sentry.webhook-url}")
    private String webhookUrl;

    @Value("${spring.profiles.active:unknown}")
    private String activeProfile;

    @Value("${custom.slack.duplicate-interval-ms:600000}")
    private long duplicateIntervalMs;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ConcurrentHashMap<String, Long> recentExceptions = new ConcurrentHashMap<>();

    public void notifyError(String title, Exception e) {
        Sentry.captureException(e);
        String exceptionType = e.getClass().getSimpleName();
        long now = System.currentTimeMillis();

        Long lastSentTime = recentExceptions.get(exceptionType);
        if (lastSentTime != null && now - lastSentTime < duplicateIntervalMs) {
            return;
        }

        recentExceptions.put(exceptionType, now);

        String fullTitle = "[%s] %s (%s)".formatted(
                activeProfile.toUpperCase(),
                title,
                exceptionType
        );

        String payload = """
            {
              "attachments": [
                {
                  "color": "danger",
                  "text": "🚨 *%s*",
                  "fields": [
                    {
                      "title": "Exception Type",
                      "value": "%s",
                      "short": true
                    },
                    {
                      "title": "Message",
                      "value": "%s",
                      "short": false
                    }
                  ],
                  "ts": "%d"
                }
              ]
            }
            """.formatted(
                escape(fullTitle),
                exceptionType,
                escape(e.getMessage()),
                now / 1000
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(webhookUrl, request, String.class);
        } catch (Exception slackError) {
            System.err.println("Slack 전송 실패: " + slackError.getMessage());
        }
    }

    private String escape(String input) {
        return input.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
