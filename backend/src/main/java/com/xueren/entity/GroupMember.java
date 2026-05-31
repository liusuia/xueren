package com.xueren.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "group_member")
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private Integer role;

    @Column(name = "is_muted")
    private Integer isMuted;

    @Column(length = 50)
    private String nickname;

    @Column(length = 50)
    private String remark;

    @Column(name = "muted_until")
    private LocalDateTime mutedUntil;

    @Column(name = "is_notification_muted")
    private Integer isNotificationMuted;

    @Column(name = "joined_at", insertable = false, updatable = false)
    private LocalDateTime joinedAt;
}
