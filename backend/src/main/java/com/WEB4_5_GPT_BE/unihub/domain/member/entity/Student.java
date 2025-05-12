package com.WEB4_5_GPT_BE.unihub.domain.member.entity;

import com.WEB4_5_GPT_BE.unihub.domain.university.entity.Major;
import com.WEB4_5_GPT_BE.unihub.domain.university.entity.University;
import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@DiscriminatorValue("STUDENT")
public class Student extends Member {

    @ManyToOne(fetch = FetchType.LAZY) // 다:1 University
    @JoinColumn(name = "university_id")
    private University university;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id")
    private Major major;

    @Column(name = "student_code", length = 20)
    private String studentCode;

    private Integer grade; // 1,2,3,4 …

    private Integer semester; // 1 or 2

    // SINGLE_TABLE로 Member를 상속받았기 때문에, 서브클래스의 필드는 반드시 nullable이어야 한다.
    // JPA에게 null check를 위임하는 대신, 엔티티를 영속하기 전에 비즈니스 로직으로 null check를 수행
    @PrePersist
    public void prePersist() {
        if (university == null || major == null || studentCode == null || grade == null || semester == null) {
            throw new UnihubException("500", "하나 이상의 필수 학생 정보가 null입니다.");
        }
    }
}
