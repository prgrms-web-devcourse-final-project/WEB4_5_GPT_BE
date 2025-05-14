package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.TimetableItemScheduleResponse;

import java.util.List;

public record TimetableItemResponse(
        Long timetableItemId,
        String type, // "COURSE" or "NORMAL"
        String title,
        Long courseId, // type이 NORMAL일 경우 null
        String location,
        String memo,
        List<TimetableItemScheduleResponse> schedule
) {}