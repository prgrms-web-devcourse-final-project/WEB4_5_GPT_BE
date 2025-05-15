package com.WEB4_5_GPT_BE.unihub.domain.course.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

public class ProfessorScheduleConflictException extends UnihubException {
    public ProfessorScheduleConflictException() {
        super("409", "강사/교수가 이미 수업 중입니다.");
    }
}
