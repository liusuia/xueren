package com.xueren.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@Entity @Table(name = "moment_comment")
public class MomentComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "moment_id", nullable = false)
    private Long momentId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(length = 500, nullable = false)
    private String content;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
