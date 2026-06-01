package com.xueren.repository;

import com.xueren.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            SELECT m FROM Message m
            WHERE m.chatType = 1
              AND ((m.fromUserId = :userId AND m.toUserId = :peerId)
                OR (m.fromUserId = :peerId AND m.toUserId = :userId))
              AND (:clearedAt IS NULL OR m.createdAt > :clearedAt)
              AND (:beforeId IS NULL OR m.id < :beforeId)
            ORDER BY m.id DESC
            """)
    List<Message> findSingleChat(@Param("userId") Long userId,
                                 @Param("peerId") Long peerId,
                                 @Param("clearedAt") LocalDateTime clearedAt,
                                 @Param("beforeId") Long beforeId,
                                 Pageable pageable);

    @Query("""
            SELECT m FROM Message m
            WHERE m.chatType = 2 AND m.groupId = :groupId
              AND (:clearedAt IS NULL OR m.createdAt > :clearedAt)
              AND (:beforeId IS NULL OR m.id < :beforeId)
            ORDER BY m.id DESC
            """)
    List<Message> findGroupChat(@Param("groupId") Long groupId,
                                 @Param("clearedAt") LocalDateTime clearedAt,
                                 @Param("beforeId") Long beforeId,
                                 Pageable pageable);

    @Query("""
            SELECT m FROM Message m
            WHERE m.content LIKE %:keyword%
              AND (
                (m.chatType = 1 AND (m.fromUserId = :userId OR m.toUserId = :userId))
                OR
                (m.chatType = 2 AND m.groupId IN (
                  SELECT gm.groupId FROM GroupMember gm WHERE gm.userId = :userId
                ))
              )
            ORDER BY m.createdAt DESC
            """)
    List<Message> searchByContent(@Param("userId") Long userId,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    @Query(value = """
            SELECT m.* FROM message m
            WHERE MATCH(m.content) AGAINST(:keyword IN BOOLEAN MODE)
              AND (
                (m.chat_type = 1 AND (m.from_user_id = :userId OR m.to_user_id = :userId))
                OR
                (m.chat_type = 2 AND m.group_id IN (
                  SELECT gm.group_id FROM group_member gm WHERE gm.user_id = :userId
                ))
              )
            ORDER BY m.created_at DESC
            LIMIT 50
            """, nativeQuery = true)
    List<Message> fulltextSearch(@Param("userId") Long userId,
                                 @Param("keyword") String keyword);

    // 清空单聊聊天记录
    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE m.chatType = 1 AND ((m.fromUserId = :userId AND m.toUserId = :peerId) OR (m.fromUserId = :peerId AND m.toUserId = :userId))")
    int deleteSingleChat(@Param("userId") Long userId, @Param("peerId") Long peerId);

    // 清空群聊聊天记录
    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE m.chatType = 2 AND m.groupId = :groupId")
    int deleteGroupChat(@Param("groupId") Long groupId);
}
