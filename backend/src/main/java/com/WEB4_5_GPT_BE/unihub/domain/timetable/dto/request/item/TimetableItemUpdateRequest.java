package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.TimetableItemType;
import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseScheduleDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TimetableItemUpdateRequest(


        @NotBlank(message = "유형은 필수입니다.")
        TimetableItemType type,

        @NotBlank(message = "강의 ID는 필수입니다.")
        Long courseId,

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotBlank(message = "교수명은 필수입니다.")
        String professorName,

        @NotBlank(message = "색상은 필수입니다.")
        String color,

        @Size(max = 300, message = "메모는 최대 300자까지 입력할 수 있습니다.")
        String memo,

        String location,

        @NotNull(message = "스케줄은 필수입니다.")
        @Size(min = 1, message = "스케줄은 최소 1개 이상이어야 합니다.")
        List<@Valid CourseScheduleDto> schedule

) {
}
