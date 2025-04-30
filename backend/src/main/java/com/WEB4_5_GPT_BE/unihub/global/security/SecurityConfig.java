package com.WEB4_5_GPT_BE.unihub.global.security;

import static com.WEB4_5_GPT_BE.unihub.global.security.SecurityConstants.AUTH_WHITELIST;

import com.WEB4_5_GPT_BE.unihub.global.response.RsData;
import com.WEB4_5_GPT_BE.unihub.global.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomAuthenticationFilter customAuthenticationFilter;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .authorizeHttpRequests(
            (authorizeHttpRequests) ->
                authorizeHttpRequests
                    .requestMatchers("/h2-console/**")
                    .permitAll()
                    .requestMatchers(AUTH_WHITELIST.toArray(String[]::new))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .headers(
            (headers) ->
                headers.addHeaderWriter(
                    new XFrameOptionsHeaderWriter(
                        XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)))
        .csrf(AbstractHttpConfigurer::disable)
        .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          response.setContentType("application/json;charset=UTF-8");
                          response.setStatus(401);
                          response
                              .getWriter()
                              .write(Ut.Json.toString(new RsData<>("401", "로그인이 필요합니다.")));
                        })
                    .accessDeniedHandler(
                        (request, response, authException) -> {
                          response.setContentType("application/json;charset=UTF-8");
                          response.setStatus(403);
                          response
                              .getWriter()
                              .write(Ut.Json.toString(new RsData<>("403", "권한이 없습니다.")));
                        }));
    ;
    return http.build();
  }
}
