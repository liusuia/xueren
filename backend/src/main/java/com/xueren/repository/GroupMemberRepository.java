package com.xueren.repository;

import com.xueren.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroupId(Long groupId);

    List<GroupMember> findByUserId(Long userId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    void deleteByGroupId(Long groupId);

    List<GroupMember> findByGroupIdAndRole(Long groupId, Integer role);

    long countByGroupIdAndRole(Long groupId, Integer role);

    @Modifying
    @Query("UPDATE GroupMember m SET m.nickname = :nickname WHERE m.groupId = :groupId AND m.userId = :userId")
    void updateNickname(Long groupId, Long userId, String nickname);

    @Modifying
    @Query("UPDATE GroupMember m SET m.role = :role WHERE m.groupId = :groupId AND m.userId = :userId")
    void updateRole(Long groupId, Long userId, Integer role);

    @Modifying
    @Query("UPDATE GroupMember m SET m.isMuted = :isMuted, m.mutedUntil = :mutedUntil WHERE m.groupId = :groupId AND m.userId = :userId")
    void updateMuteStatus(Long groupId, Long userId, Integer isMuted, LocalDateTime mutedUntil);

    @Modifying
    @Query("UPDATE GroupMember m SET m.remark = :remark WHERE m.groupId = :groupId AND m.userId = :userId")
    void updateMyGroupRemark(Long groupId, Long userId, String remark);
}
