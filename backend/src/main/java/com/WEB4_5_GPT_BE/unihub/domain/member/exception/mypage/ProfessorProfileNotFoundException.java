package com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.AuthException;

public class ProfessorProfileNotFoundException extends AuthException {
    public ProfessorProfileNotFoundException() {
        super("404", "교수 프로필을 찾을 수 없습니다.");
    }
}