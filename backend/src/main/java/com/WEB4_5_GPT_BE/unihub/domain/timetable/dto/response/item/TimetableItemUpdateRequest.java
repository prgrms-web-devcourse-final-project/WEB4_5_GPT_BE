package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TimetableItemUpdateRequest(

        @NotNull(message = "시간표 항목 ID는 필수입니다.")
        Long timetableItemId,

        @NotBlank(message = "유형은 필수입니다.")
        String type,

        Long courseId,

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        String professorName,

        @NotBlank(message = "색상은 필수입니다.")
        String color,

        @Size(max = 300, message = "메모는 최대 300자까지 입력할 수 있습니다.")
        String memo,

        String location,

        @NotNull(message = "스케줄은 필수입니다.")
        @Size(min = 1, message = "스케줄은 최소 1개 이상이어야 합니다.")
        List<@Valid ScheduleDto> schedule

) {

    public record ScheduleDto(
            @NotBlank(message = "요일은 필수입니다.")
            String day,

            @NotBlank(message = "시작 시간은 필수입니다.")
            @Pattern(
                    regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$",
                    message = "시작 시간은 HH:mm 형식이어야 합니다."
            )
            String startTime,

            @NotBlank(message = "종료 시간은 필수입니다.")
            @Pattern(
                    regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$",
                    message = "종료 시간은 HH:mm 형식이어야 합니다."
            )
            String endTime
    ) {}
}