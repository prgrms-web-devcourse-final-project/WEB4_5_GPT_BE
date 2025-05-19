package com.WEB4_5_GPT_BE.unihub.domain.course.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

public class LocationScheduleConflictException extends UnihubException {
    public LocationScheduleConflictException() {
        super("409", "강의 장소가 이미 사용 중입니다.");
    }
}
