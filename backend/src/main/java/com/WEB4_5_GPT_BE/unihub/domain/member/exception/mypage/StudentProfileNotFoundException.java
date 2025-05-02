package com.WEB4_5_GPT_BE.unihub.domain.member.exception.mypage;

import com.WEB4_5_GPT_BE.unihub.global.exception.AuthException;

public class StudentProfileNotFoundException extends AuthException {
    public StudentProfileNotFoundException() {
        super("404", "학생 프로필을 찾을 수 없습니다.");
    }
}