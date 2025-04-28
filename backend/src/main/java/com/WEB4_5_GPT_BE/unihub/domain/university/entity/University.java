package com.WEB4_5_GPT_BE.unihub.domain.university.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "university")
public class University extends BaseTimeEntity {

  @Id @GeneratedValue private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  /* 양방향 ⇒ majors */
  @OneToMany(mappedBy = "university", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<Major> majors = new ArrayList<>();
}
