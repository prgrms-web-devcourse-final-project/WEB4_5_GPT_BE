package com.WEB4_5_GPT_BE.unihub.domain.course.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

/**
 * 조회하려는 강의가 존재하지 않을 때 발생하는 예외입니다.
 */
public class CourseNotFoundException extends UnihubException {
    public CourseNotFoundException() {
        super("404", "해당 강의가 존재하지 않습니다.");
    }
}
