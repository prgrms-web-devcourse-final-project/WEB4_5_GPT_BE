package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response.item.TimetableItemResponse;

import java.util.List;

public record TimetableDetailResponse(
        Long timetableId,
        int year,
        int semester,
        List<TimetableItemResponse> timetableItems
) {}