package com.WEB4_5_GPT_BE.unihub.domain.member.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.BaseTimeEntity;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "professor_profile")
public class ProfessorProfile extends BaseTimeEntity {

  @Id private Integer memberId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  @Column(name = "employee_id", nullable = false, unique = true, length = 20)
  private String employeeId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "university_id", nullable = false)
  private University university;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "major_id", nullable = false)
  private Major major;
}
