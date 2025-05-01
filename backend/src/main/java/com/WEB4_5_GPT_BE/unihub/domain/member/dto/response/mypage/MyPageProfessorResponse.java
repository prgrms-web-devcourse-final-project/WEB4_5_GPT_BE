package com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage;

import com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage.common.MemberInfo;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Member;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.ProfessorProfile;
import lombok.Builder;

@Builder
public record MyPageProfessorResponse(
        MemberInfo member,
        ProfessorProfileResponse professorProfile
) {
    public static MyPageProfessorResponse from(Member member, ProfessorProfile profile) {
        return new MyPageProfessorResponse(
                MemberInfo.builder()
                        .id(member.getId())
                        .email(member.getEmail())
                        .name(member.getName())
                        .role(member.getRole())
                        .createdAt(member.getCreatedAt())
                        .build(),
                ProfessorProfileResponse.from(profile)
        );
    }
}