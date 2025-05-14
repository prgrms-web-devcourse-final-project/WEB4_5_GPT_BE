package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response;

public record TimetableItemScheduleResponse(
        String day,         // "MON", "TUE", ...
        String startTime,   // "HH:mm"
        String endTime      // "HH:mm"
) {}