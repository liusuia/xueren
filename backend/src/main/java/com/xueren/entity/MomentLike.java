package com.xueren.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity @Table(name = "moment_like", uniqueConstraints = @UniqueConstraint(columnNames = {"moment_id", "user_id"}))
public class MomentLike {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "moment_id", nullable = false)
    private Long momentId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;
}
