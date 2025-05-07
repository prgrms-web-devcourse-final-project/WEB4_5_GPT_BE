package com.WEB4_5_GPT_BE.unihub.domain.enrollment.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.BaseTimeEntity;
import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.member.entity.StudentProfile;
import jakarta.persistence.*;
import lombok.*;

/**
 * 학생이 신청한 강의(Enrollment) 정보를 저장하는 엔티티입니다.
 * <p>
 * 하나의 Enrollment에는 하나의 Course와 하나의 StudentProfile을 매핑하며,
 * 동일 학생이 동일 강의를 중복 신청하지 않도록
 * course_id + student_id 컬럼에 유니크 제약을 적용합니다.
 */
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "enrollment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "student_id"})
)
public class Enrollment extends BaseTimeEntity {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;           // 신청된 강의 정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;  // 수강신청한 학생 프로필
}
