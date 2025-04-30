package com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage;

import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.common.MemberInfo;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import lombok.Builder;

@Builder
public record MyPageStudentResponse(
        MemberInfo member,
        StudentProfileResponse studentProfile
) {
    public static MyPageStudentResponse from(Member member, StudentProfile profile) {
        return new MyPageStudentResponse(
                MemberInfo.builder()
                        .id(member.getId())
                        .email(member.getEmail())
                        .name(member.getName())
                        .role(member.getRole())
                        .createdAt(member.getCreatedAt())
                        .build(),
                StudentProfileResponse.from(profile)
        );
    }
}