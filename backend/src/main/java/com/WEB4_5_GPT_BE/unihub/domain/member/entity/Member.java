package com.WEB4_5_GPT_BE.unihub.domain.member.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role")
// TODO: Lombok의 @SuperBuilder 애노테이션은 @Builder와 동시에 사용하지 못하기 때문에 BaseTimeEntity를 상속받는 대신 auditing 필드를 직접 추가
@EntityListeners(AuditingEntityListener.class)
@Table(name = "member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"role", "email"}))
public abstract class Member {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "role", nullable = false, insertable = false, updatable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false; // 탈퇴 여부

    @Column
    private LocalDateTime deletedAt; // 탈퇴일

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime modifiedAt;

    // 소프트 삭제 처리 메서드
    public void markDeleted() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
