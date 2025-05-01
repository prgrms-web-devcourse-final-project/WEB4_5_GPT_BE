package com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage;

import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import lombok.Builder;

@Builder
public record StudentProfileResponse(
        String studentCode,
        String university,
        String major,
        Integer grade,
        Integer semester
) {
    public static StudentProfileResponse from(StudentProfile profile) {
        return StudentProfileResponse.builder()
                .studentCode(profile.getStudentCode())
                .university(profile.getUniversity().getName())
                .major(profile.getMajor().getName())
                .grade(profile.getGrade())
                .semester(profile.getSemester())
                .build();
    }
}
