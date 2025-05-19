package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response;

public record TimetableSemesterResponse(
        Long timetableId,
        int year,
        int semester
) {}