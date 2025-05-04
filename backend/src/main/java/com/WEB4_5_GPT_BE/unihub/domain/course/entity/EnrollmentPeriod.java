package com.WEB4_5_GPT_BE.unihub.domain.course.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.BaseTimeEntity;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "enrollment_period",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_enrollment_period_university_year_semester",
                        columnNames = {"university_id", "`year`", "semester"}
                )
        }
)
public class EnrollmentPeriod extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private University university;

    @Column(name = "`year`")
    private Integer year;
    private Integer grade;
    private Integer semester;
    private LocalDate startDate;
    private LocalDate endDate;
}
