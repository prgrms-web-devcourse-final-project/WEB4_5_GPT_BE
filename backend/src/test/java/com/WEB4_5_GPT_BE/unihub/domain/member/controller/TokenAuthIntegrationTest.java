package com.WEB4_5_GPT_BE.unihub.domain.member.controller;

import com.WEB4_5_GPT_BE.unihub.global.util.Ut;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class TokenAuthIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Value("${custom.jwt.secret-key}")
    private String secretKey;

    @Test
    @DisplayName("만료된 accessToken으로 요청하면 401-1 반환")
    void expiredAccessToken_shouldReturn401_1() throws Exception {
        // 만료된 accessToken (유효기간 -10초)
        String expiredToken = Ut.Jwt.createToken(secretKey, -10, Map.of("id", 1L, "email", "test@auni.ac.kr"));

        mockMvc.perform(get("/api/members/me/student")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401-1"))
                .andExpect(jsonPath("$.message").value("AccessToken이 만료되었습니다."));
    }
}
