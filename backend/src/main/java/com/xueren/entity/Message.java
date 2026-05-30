package com.xueren.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_type", nullable = false)
    private Integer chatType;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "to_user_id")
    private Long toUserId;

    @Column(name = "group_id")
    private Long groupId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "msg_type")
    private Integer msgType;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "is_recalled")
    private Integer isRecalled;

    @Column(name = "mention_user_ids", length = 500)
    private String mentionUserIds;

    @Column(name = "recalled_at")
    private LocalDateTime recalledAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isRecalled == null) {
            isRecalled = 0;
        }
    }
}
