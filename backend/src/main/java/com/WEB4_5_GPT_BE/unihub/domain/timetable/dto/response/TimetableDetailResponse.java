package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.Timetable;

import java.util.List;
import java.util.stream.Collectors;

public record TimetableDetailResponse(
        Long timetableId,
        int year,
        int semester,
        List<TimetableItemResponse> timetableItems
) {
    public static TimetableDetailResponse of(Timetable timetable) {
        return new TimetableDetailResponse(
                timetable.getId(),
                timetable.getYear(),
                timetable.getSemester(),
                timetable.getItems().stream()
                        .map(TimetableItemResponse::of) // 이 메서드도 있어야 함
                        .collect(Collectors.toList())
        );
    }
}
