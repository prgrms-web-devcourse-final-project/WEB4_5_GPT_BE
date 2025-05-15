package com.WEB4_5_GPT_BE.unihub.domain.course.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.DayOfWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.catchThrowable;

@DisplayName("강의 스케줄 엔티티 테스트")
class CourseScheduleTest {

    @Test
    @DisplayName("빌더에 올바른 값을 넘겨주면 오브젝트가 정상적으로 생성된다.")
    void givenValidDate_whenInstantiatingCourseScheduleWithBuilder_thenSucceeds() {
        CourseSchedule courseSchedule = CourseSchedule.builder()
                .id(123L)
                .course(new Course())
                .universityId(321L)
                .location("testLocation")
                .professorProfileEmployeeId("testEmpId")
                .day(DayOfWeek.SAT)
                .startTime(LocalTime.parse("12:34"))
                .endTime(LocalTime.parse("23:45"))
                .build();

        assertThat(courseSchedule.getId()).isEqualTo(123L);
        assertThat(courseSchedule.getCourse()).isInstanceOf(Course.class);
        assertThat(courseSchedule.getUniversityId()).isEqualTo(321L);
        assertThat(courseSchedule.getLocation()).isEqualTo("testLocation");
        assertThat(courseSchedule.getProfessorProfileEmployeeId()).isEqualTo("testEmpId");
        assertThat(courseSchedule.getDay()).isEqualTo(DayOfWeek.SAT);
        assertThat(courseSchedule.getStartTime()).isEqualTo(LocalTime.parse("12:34"));
        assertThat(courseSchedule.getEndTime()).isEqualTo(LocalTime.parse("23:45"));
    }

    @Test
    @DisplayName("빌더에 수업 종료 시각보다 늦은 시작 시각을 넘겨주면 예외를 던진다.")
    void givenImpossibleTimeframe_whenInstantiatingCourseScheduleWithBuilder_thenThrowsException() {
        Throwable thrown = catchThrowable(
                () -> CourseSchedule.builder()
                        .course(new Course())
                        .universityId(1L)
                        .location("testLocation")
                        .day(DayOfWeek.MON)
                        .startTime(LocalTime.parse("13:00"))
                        .endTime(LocalTime.parse("11:00"))
                        .build()
        );

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수업 시작 시각이 종료 시각보다 늦습니다.");
    }
}