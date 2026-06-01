package com.xueren.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "emoji")
public class Emoji {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
