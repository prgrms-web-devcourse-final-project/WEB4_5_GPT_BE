package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TimetableCourseAddRequest(

        @NotNull(message = "시간표 ID는 필수입니다.")
        Long timetableId,

        @NotNull(message = "강의 ID는 필수입니다.")
        Long courseId,

        @NotBlank(message = "색상은 필수입니다.")
        String color,

        @Size(max = 300, message = "메모는 최대 300자까지 입력할 수 있습니다.")
        String memo

) {}