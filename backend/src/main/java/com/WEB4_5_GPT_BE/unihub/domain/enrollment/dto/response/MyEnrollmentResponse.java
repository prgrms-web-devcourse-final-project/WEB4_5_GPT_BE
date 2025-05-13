package com.WEB4_5_GPT_BE.unihub.domain.enrollment.dto.response;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity.Enrollment;
import lombok.Builder;

import java.util.List;

/**
 * 회원(학생)이 신청한 강의 목록 정보를 담아
 * 클라이언트로 전송하기 위한 DTO입니다.
 */
@Builder
public record MyEnrollmentResponse(
        Long enrollmentId,                     // 수강신청 고유 ID
        Long courseId,                         // 강의 고유 ID
        String majorName,                      // 강의 소속 전공명
        String courseTitle,                    // 강의 제목
        String professorName,                  // 담당 교수명
        String location,                       // 강의실 위치
        List<CourseScheduleResponse> schedule, // 강의 시간표 목록
        Integer credit,                        // 학점
        Integer grade,                         // 대상 학년
        Integer semester,                      // 대상 학기
        Integer capacity,                      // 강의 정원
        Integer availableSeats                 // 남은 좌석 수 (최대 인원 - 현재 신청 인원)
) {
    /**
     * 수강 신청 정보를 MyEnrollmentResponse DTO로 변환한다.
     *
     * @param enrollment 변환할 수강신청 정보
     * @return 변환된 MyEnrollmentResponse DTO 객체
     */
    public static MyEnrollmentResponse from(Enrollment enrollment) {

        // 수강 신청 정보에서 강의 정보를 가져온다.
        Course course = enrollment.getCourse();

        // 해당 강의에 대한 모든 스케줄 정보를 가져온다.
        List<CourseScheduleResponse> schedules = course.getSchedules()
                .stream()
                .map(CourseScheduleResponse::from) // 각 강의 스케줄 정보를 DTO로 변환
                .toList();

        return MyEnrollmentResponse.builder()
                .enrollmentId(enrollment.getId())
                .courseId(course.getId())
                .majorName(course.getMajor().getName())
                .courseTitle(course.getTitle())
                .professorName(course.getProfessor().getName())
                .location(course.getLocation())
                .schedule(schedules)
                .credit(course.getCredit())
                .grade(course.getGrade())
                .semester(course.getSemester())
                .capacity(course.getCapacity())
                .availableSeats(course.getAvailableSeats())
                .build();
    }
}
