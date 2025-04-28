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
@Table(
    name = "student_profile",
    uniqueConstraints = @UniqueConstraint(columnNames = {"university_id", "student_code"}))
public class StudentProfile extends BaseTimeEntity {

  @Id private Integer memberId; // PK와 FK를 공유 (1:1)

  @MapsId
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  @Column(name = "student_code", nullable = false, length = 20)
  private String studentCode;

  @ManyToOne(fetch = FetchType.LAZY) // 다:1 University
  @JoinColumn(name = "university_id", nullable = false)
  private University university;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "major_id", nullable = false)
  private Major major;

  @Column(nullable = false)
  private Integer grade; // 1,2,3,4 …

  @Column(nullable = false)
  private Integer semester; // 1 or 2
}
