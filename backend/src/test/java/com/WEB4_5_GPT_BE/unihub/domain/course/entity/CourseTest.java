package com.WEB4_5_GPT_BE.unihub.domain.course.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("강의 엔티티 오브젝트 테스트")
class CourseTest {

    @Test
    @DisplayName("스케줄을 가지고 있는 강의의 스케줄 요약 문자열을 올바르게 반환한다.")
    void givenCourseWithSchedule_whenRequestingSummaryString_thenReturnCorrectSummaryString() {
        Course course = new Course();
        Stream.of(
                new CourseSchedule(1L, course, 1L, "testLocation", "", DayOfWeek.MON, LocalTime.parse("12:00"), LocalTime.parse("13:00")),
                new CourseSchedule(1L, course, 1L, "testLocation", "", DayOfWeek.WED, LocalTime.parse("12:00"), LocalTime.parse("13:00")),
                new CourseSchedule(1L, course, 1L, "testLocation", "", DayOfWeek.THU, LocalTime.parse("14:00"), LocalTime.parse("15:00"))
        ).forEach(cs -> course.getSchedules().add(cs));

        String res = course.scheduleToString();
        assertEquals("MON12-13WED12-13THU14-15", res);
    }

    @Test
    @DisplayName("스케줄이 없는 강의의 스케줄 요약 문자열로 빈 문자열을 반환한다.")
    void givenCourseWithoutSchedule_whenRequestingSummaryString_thenReturnEmptyString() {
        Course course = new Course();
        String res = course.scheduleToString();
        assertEquals("", res);
    }
}