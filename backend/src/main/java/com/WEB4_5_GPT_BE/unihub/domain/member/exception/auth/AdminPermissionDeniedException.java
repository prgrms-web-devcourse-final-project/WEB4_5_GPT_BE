package com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.AuthException;

public class AdminPermissionDeniedException extends AuthException {
    public AdminPermissionDeniedException() {
        super("403", "관리자 권한이 없습니다.");
    }
}