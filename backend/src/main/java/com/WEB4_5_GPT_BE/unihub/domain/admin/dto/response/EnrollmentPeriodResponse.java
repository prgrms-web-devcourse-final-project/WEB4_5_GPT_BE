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
public class EnrollmentPeriodResponse {
    private Long id;
    private String universityName;
    private Integer grade;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
