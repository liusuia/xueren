package com.xueren.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@Entity @Table(name = "red_packet_receive", uniqueConstraints = @UniqueConstraint(columnNames = {"packet_id", "user_id"}))
public class RedPacketReceive {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "packet_id", nullable = false)
    private Long packetId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Integer amount;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
