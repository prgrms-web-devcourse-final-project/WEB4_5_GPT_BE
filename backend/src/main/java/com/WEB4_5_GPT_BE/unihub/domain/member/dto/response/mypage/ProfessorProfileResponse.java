package com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import lombok.Builder;

@Builder
public record ProfessorProfileResponse(
        String employeeId,
        String university,
        String major
) {
    public static ProfessorProfileResponse from(ProfessorProfile profile) {
        return ProfessorProfileResponse.builder()
                .employeeId(profile.getEmployeeId())
                .university(profile.getUniversity().getName())
                .major(profile.getMajor().getName())
                .build();
    }
}
