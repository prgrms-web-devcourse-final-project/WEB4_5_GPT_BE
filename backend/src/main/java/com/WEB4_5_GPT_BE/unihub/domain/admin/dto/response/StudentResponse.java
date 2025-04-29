package com.WEB4_5_GPT_BE.unihub.domain.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private Long memberId;
    private String universityName;
    private String memberName;
    private String studentCode;
    private String majorName;
    private Integer grade;
    private Integer semester;
    private LocalDateTime createdAt;
}
