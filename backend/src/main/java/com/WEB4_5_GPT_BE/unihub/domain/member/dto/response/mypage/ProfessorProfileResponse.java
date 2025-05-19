package com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Professor;
import lombok.Builder;

@Builder
public record ProfessorProfileResponse(
        String employeeId,
        Long universityId,
        String university,
        String major
) {
    public static ProfessorProfileResponse from(Professor profile) {
        return ProfessorProfileResponse.builder()
                .employeeId(profile.getEmployeeId())
                .universityId(profile.getUniversity().getId())
                .university(profile.getUniversity().getName())
                .major(profile.getMajor().getName())
                .build();
    }
}
