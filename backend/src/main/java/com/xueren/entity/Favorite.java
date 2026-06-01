package com.xueren.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "favorite", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "message_id"}))
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
