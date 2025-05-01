package com.WEB4_5_GPT_BE.unihub.domain.member.service;

import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.AdminLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.request.MemberLoginRequest;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.AdminLoginResponse;
import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.MemberLoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
  MemberLoginResponse login(MemberLoginRequest request);

  AdminLoginResponse adminLogin(AdminLoginRequest request);

  void logout(HttpServletRequest request, HttpServletResponse response);

  MemberLoginResponse refreshAccessToken(HttpServletRequest request, HttpServletResponse response);
}
