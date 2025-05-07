package com.WEB4_5_GPT_BE.unihub.domain.enrollment.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

/**
 * 정원이 초과되어 수강 신청이 불가능할 때 발생하는 예외입니다.
 */
public class CourseCapacityExceededException extends UnihubException {
    public CourseCapacityExceededException() {
        super("409", "정원이 초과되어 수강 신청이 불가능합니다.");
    }
}
