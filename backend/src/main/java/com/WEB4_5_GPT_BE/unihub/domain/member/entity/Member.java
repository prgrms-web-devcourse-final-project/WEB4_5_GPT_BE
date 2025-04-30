package com.WEB4_5_GPT_BE.unihub.domain.member.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.BaseTimeEntity;
import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "member", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Member extends BaseTimeEntity {

  @Id @GeneratedValue private Long id;

  @Column(nullable = false, unique = true, length = 120)
  private String email;

  @Column(nullable = false, length = 255)
  private String password;

  @Column(nullable = false, length = 30)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @OneToOne(
      mappedBy = "member",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private StudentProfile studentProfile; // role == STUDENT일 때만 존재

  @OneToOne(
      mappedBy = "member",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private ProfessorProfile professorProfile; // role == PROFESSOR일 때만 존재
}
