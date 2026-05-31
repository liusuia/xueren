package com.xueren.repository;

import com.xueren.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByUserIdOrderByLastMessageAtDesc(Long userId);

    Optional<Conversation> findByUserIdAndTargetTypeAndTargetId(Long userId, Integer targetType, Long targetId);

    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, Integer targetType, Long targetId);

    /** 原子递增未读数，避免读-改-写竟态 */
    @Modifying
    @Query("UPDATE Conversation c SET c.unreadCount = COALESCE(c.unreadCount, 0) + 1 WHERE c.id = :convId")
    int incrementUnread(@Param("convId") Long convId);
}
