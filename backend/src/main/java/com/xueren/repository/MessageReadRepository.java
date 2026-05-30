package com.xueren.repository;

import com.xueren.entity.MessageRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageReadRepository extends JpaRepository<MessageRead, Long> {

    Optional<MessageRead> findByMessageIdAndUserId(Long messageId, Long userId);

    long countByMessageId(Long messageId);
}
