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

    /** 更新会话元信息（不覆盖未读数） */
    @Modifying
    @Query("UPDATE Conversation c SET c.lastMessageId=:msgId, c.lastMessagePreview=:preview, c.lastMessageAt=:time WHERE c.id=:id")
    void updateMeta(@Param("id") Long id, @Param("msgId") Long msgId, @Param("preview") String preview, @Param("time") java.time.LocalDateTime time);
}
