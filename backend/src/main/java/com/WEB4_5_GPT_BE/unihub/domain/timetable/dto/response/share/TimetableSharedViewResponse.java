package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.share;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemResponse;

import java.util.List;

public record TimetableSharedViewResponse(
        Long timetableId,
        int year,
        int semester,
        String ownerName,
        List<TimetableItemResponse> timetables
) {}