package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.TimetableItemScheduleResponse;
import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.TimetableItem;

import java.util.List;
import java.util.stream.Collectors;


public record TimetableItemResponse(
        Long timetableItemId,
        String type,
        String title,
        Long courseId,
        String location,
        String memo,
        List<TimetableItemScheduleResponse> schedule
) {
    public static TimetableItemResponse of(TimetableItem item) {
        return new TimetableItemResponse(
                item.getId(),
                item.getType().name(), // enum이면 .name()으로 문자열 변환
                item.getTitle(),
                item.getCourse() != null ? item.getCourse().getId() : null,
                item.getLocation(),
                item.getMemo(),
                item.getSchedules().stream()
                        .map(TimetableItemScheduleResponse::of)
                        .collect(Collectors.toList())
        );
    }
}