package com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage;

import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import lombok.Builder;

@Builder
public record UpdateMajorResponse(
        Long majorId,
        String majorName
) {
    public static UpdateMajorResponse from(Major major) {
        return UpdateMajorResponse.builder()
                .majorId(major.getId())
                .majorName(major.getName())
                .build();
    }
}
