package com.WEB4_5_GPT_BE.unihub.global.security;

import java.util.List;

public class SecurityConstants {
  public static final List<String> AUTH_WHITELIST =
      List.of(
          "/api/members/signup/student",
          "/api/members/signup/professor",
          "/api/members/email/SIGNUP/code",
          "/api/members/email/SIGNUP/verify",
          "/api/members/email/PASSWORD_RESET/code",
              "/api/members/email/PASSWORD_RESET/verify",
          "/api/members/login",
          "/api/members/login/admin",
          "/api/members/refresh",
          "/api/members/password-reset/confirm",
          "/api/majors/**",
          "/api/universities/**",
          "/swagger-ui/**",
          "/v3/api-docs/**",
          "/h2-console/**",
              "/actuator/**",
              "/api/file/**"
      );
}
