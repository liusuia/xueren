package com.xueren.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@Entity @Table(name = "red_packet")
public class RedPacket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    @Column(name = "chat_type", nullable = false)
    private Integer chatType;
    @Column(name = "target_id", nullable = false)
    private Long targetId;
    @Column(nullable = false)
    private Integer amount; // 分
    @Column(nullable = false)
    private Integer count;
    @Column(name = "remaining_count")
    private Integer remainingCount;
    @Column(length = 50)
    private String message;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
