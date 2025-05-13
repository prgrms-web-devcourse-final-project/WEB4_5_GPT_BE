package com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.EnrollmentPeriod;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.Student;
import lombok.Builder;

import java.time.LocalDate;

/**
 * 수강신청 기간 응답 DTO
 */
@Builder
public record StudentEnrollmentPeriodResponse(
        Long studentId,
        String universityName,
        Integer year,
        Integer grade,
        Integer semester,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isEnrollmentOpen
) {

    /**
     * 수강신청 기간 응답 DTO 생성 (isEnrollmentOpen 포함 (수강신청 기간 내 요청 여부))
     */
    public static StudentEnrollmentPeriodResponse from(
            EnrollmentPeriod period,
            Student profile,
            boolean isEnrollmentOpen
    ) {
        return StudentEnrollmentPeriodResponse.builder()
                .studentId(profile.getId())
                .universityName(profile.getUniversity().getName())
                .year(period.getYear())
                .grade(period.getGrade())
                .semester(period.getSemester())
                .startDate(period.getStartDate())
                .endDate(period.getEndDate())
                .isEnrollmentOpen(isEnrollmentOpen)
                .build();
    }

    /**
     * 기간이 repository에서 조회되지 않을 경우 사용
     */
    public static StudentEnrollmentPeriodResponse notOpen() {
        return StudentEnrollmentPeriodResponse.builder()
                .isEnrollmentOpen(false)
                .build();
    }
}