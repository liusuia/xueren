package com.xueren.repository;

import com.xueren.entity.RedPacketReceive;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RedPacketReceiveRepository extends JpaRepository<RedPacketReceive, Long> {
    List<RedPacketReceive> findByPacketId(Long packetId);
    boolean existsByPacketIdAndUserId(Long packetId, Long userId);
}
