package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorResponse {
    private Long memberId;
    private String universityName;
    private String memberName;
    private String majorName;
    private ApprovalStatus status;
    private LocalDateTime createdAt;
}
