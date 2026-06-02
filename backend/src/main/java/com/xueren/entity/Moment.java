package com.xueren.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@Entity @Table(name = "moment")
public class Moment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(length = 2000)
    private String content;
    @Column(length = 2000)
    private String images; // JSON array of URLs
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
