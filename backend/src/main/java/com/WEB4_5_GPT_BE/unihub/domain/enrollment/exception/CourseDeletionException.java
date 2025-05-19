package com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

public class CourseDeletionException extends UnihubException {
    public CourseDeletionException() {
        super("400", "수강신청이 한 개라도 되어있는 강의는 삭제할 수 없습니다.");
    }
}