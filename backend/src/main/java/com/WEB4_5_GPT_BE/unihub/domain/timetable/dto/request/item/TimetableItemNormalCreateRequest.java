package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TimetableItemNormalCreateRequest(

        @NotNull(message = "시간표 ID는 필수입니다.")
        Long timetableId,

        @NotBlank(message = "일정 제목은 필수입니다.")
        String title,

        String professorName,

        @NotBlank(message = "색상은 필수입니다.")
        String color,

        String location,

        @Size(max = 300, message = "메모는 최대 300자까지 입력할 수 있습니다.")
        String memo,

        @NotNull(message = "스케줄 정보는 필수입니다.")
        @Size(min = 1, message = "최소 하나 이상의 스케줄이 필요합니다.")
        List<@Valid ScheduleRequest> schedule

) {
    public record ScheduleRequest(
            @NotNull(message = "요일은 필수입니다.")
            DayOfWeek day,

            @NotBlank(message = "시작 시간은 필수입니다.")
            @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "시작 시간은 HH:mm 형식이어야 합니다.")
            String startTime,

            @NotBlank(message = "종료 시간은 필수입니다.")
            @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "시작 시간은 HH:mm 형식이어야 합니다.")
            String endTime
    ) {}
}