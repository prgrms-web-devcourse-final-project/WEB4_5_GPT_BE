package com.WEB4_5_GPT_BE.unihub.domain.member.exception.auth;

import com.WEB4_5_GPT_BE.unihub.domain.member.exception.AuthException;

public class ProfessorNotApprovedException  extends AuthException {
    public ProfessorNotApprovedException() { super("401", "아직 승인이 완료되지 않은 교직원 계정입니다."); }
}

