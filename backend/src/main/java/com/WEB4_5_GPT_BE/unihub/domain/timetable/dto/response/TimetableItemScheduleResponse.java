package com.WEB4_5_GPT_BE.unihub.domain.timetable.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.TimetableItemSchedule;

import java.time.format.DateTimeFormatter;

public record TimetableItemScheduleResponse(
        String day,         // "MON", "TUE", ...
        String startTime,   // "HH:mm"
        String endTime      // "HH:mm"
) {
    public static TimetableItemScheduleResponse of(TimetableItemSchedule schedule) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return new TimetableItemScheduleResponse(
                schedule.getDay().name(), // or .toString()
                schedule.getStartTime().format(formatter),
                schedule.getEndTime().format(formatter)
        );
    }
}