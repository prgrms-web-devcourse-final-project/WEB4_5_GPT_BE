package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item;

import java.util.List;

public record TimetableItemDetailResponse(
        int year,
        int semester,
        String title,
        String professorName,
        String location,
        String color,
        String memo,
        List<ScheduleDto> schedule
) {

    public record ScheduleDto(
            String day,
            String startTime,
            String endTime
    ) {}
}