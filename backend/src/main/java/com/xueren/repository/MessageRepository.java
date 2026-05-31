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
