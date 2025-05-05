package com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.CourseSchedule;
import lombok.Builder;

/**
 * 클라이언트에 전송할 강의 시간표 정보를 담는 DTO입니다.
 */
@Builder
public record CourseScheduleResponse(
        String day,        // 강의 요일 (MON, TUE 등)
        String startTime,  // 강의 시작 시간
        String endTime     // 강의 종료 시간
) {
    /**
     * 강의 스케줄을 CourseScheduleResponse DTO로 변환합니다.
     *
     * @param courseSchedule 변환할 강의 스케줄 정보
     * @return 변환된 CourseScheduleResponse DTO 객체
     */
    public static CourseScheduleResponse from(CourseSchedule courseSchedule) {
        return CourseScheduleResponse.builder()
                .day(courseSchedule.getDay().name())                 // 요일 enum → 문자열
                .startTime(courseSchedule.getStartTime().toString()) // LocalTime → "HH:mm:ss"
                .endTime(courseSchedule.getEndTime().toString())     // LocalTime → "HH:mm:ss"
                .build();
    }
}