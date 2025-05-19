package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.request.item;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TimetableBulkRegisterRequest(
        @NotNull(message = "시간표 항목 ID는 필수입니다.")
        Long timetableId,
        @NotNull(message = "강의 ID 리스트는 필수입니다.")
        List<Long> courseIds,
        String color,
        String memo
) {
}
