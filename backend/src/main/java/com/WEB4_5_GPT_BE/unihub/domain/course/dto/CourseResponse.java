package com.WEB4_5_GPT_BE.unihub.domain.course.dto;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;

public record CourseResponse(
        Long id,
        String title,
        String major,
        String university,
        String location,
        Integer capacity,
        Integer credit,
        String professor,
        Integer grade,
        Integer semester,
        String coursePlanAttachment,
        String schedule
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getMajor().getName(),
                course.getMajor().getUniversity().getName(),
                course.getLocation(),
                course.getCapacity(),
                course.getCredit(),
                course.getProfessor() != null ? course.getProfessor().getMember().getName() : null,
                course.getGrade(),
                course.getSemester(),
                course.getCoursePlanAttachment() != null ? course.getCoursePlanAttachment() : null,
                course.scheduleToString()
        );
    }
}
