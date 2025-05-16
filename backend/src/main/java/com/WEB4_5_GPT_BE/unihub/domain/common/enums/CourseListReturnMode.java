package com.WEB4_5_GPT_BE.unihub.domain.common.enums;


public enum CourseListReturnMode {
    FULL("full"),
    ENROLL("enroll"),
    CATALOG("catalog");

    final String description;

    CourseListReturnMode(String description) {
        this.description = description;
    }
}
