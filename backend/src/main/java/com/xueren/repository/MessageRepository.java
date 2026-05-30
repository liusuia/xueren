package com.xueren.repository;

import com.xueren.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            SELECT m FROM Message m
            WHERE m.chatType = 1 AND m.isRecalled = 0
              AND ((m.fromUserId = :userId AND m.toUserId = :peerId)
                OR (m.fromUserId = :peerId AND m.toUserId = :userId))
            ORDER BY m.id DESC
            """)
    List<Message> findSingleChat(@Param("userId") Long userId,
                                 @Param("peerId") Long peerId,
                                 Pageable pageable);

    @Query("""
            SELECT m FROM Message m
            WHERE m.chatType = 2 AND m.groupId = :groupId AND m.isRecalled = 0
            ORDER BY m.id DESC
            """)
    List<Message> findGroupChat(@Param("groupId") Long groupId, Pageable pageable);

    @Query("""
            SELECT m FROM Message m
            WHERE m.isRecalled = 0
              AND m.content LIKE %:keyword%
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
}
