package com.xueren.repository;

import com.xueren.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByUserIdOrderByLastMessageAtDesc(Long userId);

    Optional<Conversation> findByUserIdAndTargetTypeAndTargetId(Long userId, Integer targetType, Long targetId);

    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, Integer targetType, Long targetId);
}
