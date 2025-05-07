package com.WEB4_5_GPT_BE.unihub.domain.member.dto.response.mypage;

import com.WEB4_5_GPT_BE.unihub.domain.course.dto.CourseScheduleDto;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import lombok.Builder;

import java.util.List;

@Builder
public record ProfessorCourseResponse(
        Long courseId,
        String title,
        String major,
        String location,
        List<CourseScheduleDto> schedule,  // 강의 시간 여러개
        Integer capacity,
        Integer enrolled,
        Integer credit,
        Integer grade,
        Integer semester,
        String coursePlanAttachment
) {
    public static ProfessorCourseResponse from(Course course) {
        return ProfessorCourseResponse.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .major(course.getMajor().getName())
                .location(course.getLocation())
                .schedule(course.getSchedules()
                        .stream()
                        .map(CourseScheduleDto::from)
                        .toList())
                .capacity(course.getCapacity())
                .enrolled(course.getEnrolled())
                .credit(course.getCredit())
                .grade(course.getGrade())
                .semester(course.getSemester())
                .coursePlanAttachment(course.getCoursePlanAttachment())
                .build();
    }
}
