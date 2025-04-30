package com.WEB4_5_GPT_BE.unihub.domain.university.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;

public record MajorResponse(
    Long id,
    String name
) {
    public static MajorResponse from(Major major) {
        return new MajorResponse(
            major.getId(),
            major.getName()
        );
    }
}
