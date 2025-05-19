package com.WEB4_5_GPT_BE.unihub.domain.notice.entity;

import com.WEB4_5_GPT_BE.unihub.domain.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.*;

@Entity
@Table(
        name = "notice",
        indexes = {
                @Index(
                        name = "idx_notice_is_deleted_created_at",
                        columnList = "is_deleted, created_at DESC"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 2048)
    private String attachmentUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    public void softDelete() {
        this.isDeleted = true;
    }

    public void update(String title, String content, String attachmentUrl) {
        this.title = title;
        this.content = content;
        this.attachmentUrl = attachmentUrl;
    }
}