package com.xueren.service;

import com.xueren.common.BusinessException;
import com.xueren.common.Constants;
import com.xueren.dto.CreateGroupRequest;
import com.xueren.dto.GroupFileVO;
import com.xueren.dto.GroupMemberVO;
import com.xueren.dto.GroupVO;
import com.xueren.dto.MessageVO;
import com.xueren.dto.UserVO;
import com.xueren.entity.ChatGroup;
import com.xueren.entity.GroupFile;
import com.xueren.entity.GroupMember;
import com.xueren.entity.StoredFile;
import com.xueren.entity.User;
import com.xueren.repository.ChatGroupRepository;
import com.xueren.repository.GroupFileRepository;
import com.xueren.repository.GroupMemberRepository;
import com.xueren.repository.StoredFileRepository;
import com.xueren.repository.UserRepository;
import com.xueren.event.GroupEvent;
import com.xueren.event.JoinRequestEvent;
import com.xueren.netty.ChannelManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final ChatGroupRepository chatGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupFileRepository groupFileRepository;
    private final StoredFileRepository storedFileRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final ApplicationEventPublisher eventPublisher;
    private final ChannelManager channelManager;

    public GroupService(ChatGroupRepository chatGroupRepository,
                        GroupMemberRepository groupMemberRepository,
                        GroupFileRepository groupFileRepository,
                        StoredFileRepository storedFileRepository,
                        UserService userService,
                        UserRepository userRepository,
                        JdbcTemplate jdbc,
                        DataSource dataSource,
                        ApplicationEventPublisher eventPublisher,
                        ChannelManager channelManager) {
        this.chatGroupRepository = chatGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupFileRepository = groupFileRepository;
        this.storedFileRepository = storedFileRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.eventPublisher = eventPublisher;
        this.channelManager = channelManager;
    }

    @Transactional
    public GroupVO createGroup(Long ownerId, CreateGroupRequest request) {
        ChatGroup group = new ChatGroup();
        group.setName(request.getName());
        group.setOwnerId(ownerId);
        group.setGroupCode(generateGroupCode());
        chatGroupRepository.save(group);

        saveMember(group.getId(), ownerId, Constants.GROUP_ROLE_OWNER);

        List<Long> memberIds = new ArrayList<>(request.getMemberIds());
        memberIds.remove(ownerId);
        for (Long memberId : memberIds) {
            userService.requireUser(memberId);
            saveMember(group.getId(), memberId, Constants.GROUP_ROLE_MEMBER);
        }
        return getGroupDetail(group.getId());
    }

    private String generateGroupCode() {
        var random = new java.security.SecureRandom();
        for (int i = 0; i < 20; i++) {
            String code = String.format("%06d", random.nextInt(900000) + 100000);
            if (!chatGroupRepository.existsByGroupCode(code)) return code;
        }
        throw new BusinessException("生成群号失败，请重试");
    }

    public List<GroupVO> listMyGroups(Long userId) {
        return groupMemberRepository.findByUserId(userId).stream()
                .map(member -> getGroupDetail(member.getGroupId(), userId))
                .toList();
    }

    public ChatGroup getGroup(Long groupId) {
        return chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("群组不存在"));
    }

    public GroupVO getGroupDetail(Long groupId) {
        return buildGroupVO(groupId, null);
    }

    public GroupVO getGroupDetail(Long groupId, Long userId) {
        return buildGroupVO(groupId, userId);
    }

    private GroupVO buildGroupVO(Long groupId, Long userId) {
        ChatGroup group = getGroup(groupId);
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        // 批量加载所有成员用户，替代 N+1 查询
        List<Long> userIds = members.stream().map(GroupMember::getUserId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        String remark = null;
        if (userId != null) {
            remark = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                    .map(GroupMember::getRemark).orElse(null);
        }
        return GroupVO.builder()
                .id(group.getId())
                .groupCode(group.getGroupCode())
                .name(group.getName())
                .avatar(group.getAvatar())
                .ownerId(group.getOwnerId())
                .createdAt(group.getCreatedAt())
                .remark(remark)
                .notice(group.getNotice())
                .noticeUpdatedAt(group.getNoticeUpdatedAt())
                .joinMode(group.getJoinMode())
                .members(members.stream()
                        .map(m -> userMap.get(m.getUserId()))
                        .filter(Objects::nonNull)
                        .map(u -> UserVO.builder()
                                .id(u.getId())
                                .username(u.getUsername())
                                .nickname(u.getNickname())
                                .avatar(u.getAvatar())
                                .build())
                        .toList())
                .memberVOs(members.stream()
                        .map(m -> {
                            var user = userMap.get(m.getUserId());
                            if (user == null) return null;
                            var displayName = m.getNickname() != null && !m.getNickname().isBlank()
                                    ? m.getNickname()
                                    : (user.getNickname() != null ? user.getNickname() : user.getUsername());
                            return GroupMemberVO.builder()
                                    .userId(m.getUserId())
                                    .nickname(displayName)
                                    .remark(m.getRemark())
                                    .avatar(user.getAvatar())
                                    .username(user.getUsername())
                                    .role(m.getRole())
                                    .isMuted(m.getIsMuted() != null && m.getIsMuted() == 1)
                                    .isNotificationMuted(m.getIsNotificationMuted() != null && m.getIsNotificationMuted() == 1)
                                    .isOnline(channelManager.isOnline(m.getUserId()))
                                    .build();
                        })
                        .filter(Objects::nonNull)
                        .toList())
                .build();
    }

    public void ensureMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new BusinessException("不是群成员");
        }
    }

    public List<Long> listMemberUserIds(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(GroupMember::getUserId)
                .toList();
    }

    @Transactional
    public void addMember(Long operatorId, Long groupId, Long userId) {
        ChatGroup group = getGroup(groupId);
        GroupMember operator = groupMemberRepository.findByGroupIdAndUserId(groupId, operatorId)
                .orElseThrow(() -> new BusinessException("不是群成员"));
        if (operator.getRole() > Constants.GROUP_ROLE_ADMIN) {
            throw new BusinessException("没有权限邀请成员");
        }
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new BusinessException("用户已在群中");
        }
        userService.requireUser(userId);
        saveMember(groupId, userId, Constants.GROUP_ROLE_MEMBER);
        // 发系统消息：XXX邀请了YYY加入群聊
        String opName = resolveDisplayName(userRepository.findById(operatorId).orElse(null));
        String invitedName = resolveDisplayName(userRepository.findById(userId).orElse(null));
        String sysMsg = opName + "邀请了" + invitedName + "加入群聊";
        publishGroupSystemMessage(groupId, userId, sysMsg);
    }

    @Transactional
    public void addMembers(Long operatorId, Long groupId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        ChatGroup group = getGroup(groupId);
        GroupMember operator = groupMemberRepository.findByGroupIdAndUserId(groupId, operatorId)
                .orElseThrow(() -> new BusinessException("不是群成员"));
        if (operator.getRole() > Constants.GROUP_ROLE_ADMIN) {
            throw new BusinessException("没有权限邀请成员");
        }
        String opName = resolveDisplayName(userRepository.findById(operatorId).orElse(null));
        for (Long userId : userIds) {
            if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
                userService.requireUser(userId);
                saveMember(groupId, userId, Constants.GROUP_ROLE_MEMBER);
                // 发系统消息：XXX邀请了YYY加入群聊
                String invitedName = resolveDisplayName(userRepository.findById(userId).orElse(null));
                String sysMsg = opName + "邀请了" + invitedName + "加入群聊";
                publishGroupSystemMessage(groupId, userId, sysMsg);
            }
        }
    }

    private void saveMember(Long groupId, Long userId, int role) {
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        member.setIsMuted(0);
        groupMemberRepository.save(member);
    }

    private String resolveDisplayName(User user) {
        if (user == null) return "用户";
        return user.getNickname() != null ? user.getNickname() : user.getUsername();
    }

    private void publishGroupSystemMessage(Long groupId, Long fromUserId, String sysMsg) {
        jdbc.update(
            "INSERT INTO message (chat_type, from_user_id, group_id, content, msg_type, created_at) VALUES (?,?,?,?,?,NOW())",
            Constants.CHAT_GROUP, fromUserId, groupId, sysMsg, Constants.MSG_SYSTEM);
        for (var m : groupMemberRepository.findByGroupId(groupId)) {
            jdbc.update(
                "INSERT INTO conversation (user_id, target_type, target_id, last_message_preview, last_message_at) VALUES (?,2,?,?,NOW()) ON DUPLICATE KEY UPDATE last_message_preview=?, last_message_at=NOW()",
                m.getUserId(), groupId, sysMsg, sysMsg);
        }
        eventPublisher.publishEvent(new GroupEvent(groupId, sysMsg));
    }

    @Transactional
    public void quitGroup(Long userId, Long groupId) {
        ChatGroup group = getGroup(groupId);

        if (group.getOwnerId().equals(userId)) {
            throw new BusinessException("群主不能退出群组，请先转让群主");
        }

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException("不是群成员"));

        groupMemberRepository.delete(member);
    }

    @Transactional
    public void removeMember(Long operatorId, Long groupId, Long userId) {
        ChatGroup group = getGroup(groupId);

        GroupMember operator = groupMemberRepository.findByGroupIdAndUserId(groupId, operatorId)
                .orElseThrow(() -> new BusinessException("不是群成员"));
        if (operator.getRole() > Constants.GROUP_ROLE_ADMIN) {
            throw new BusinessException("没有权限移除成员");
        }

        if (group.getOwnerId().equals(userId)) {
            throw new BusinessException("不能移除群主");
        }

        if (operatorId.equals(userId)) {
            throw new BusinessException("请使用退出群组功能");
        }

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException("用户不在群中"));

        groupMemberRepository.delete(member);
        // 发系统消息：XXX已被移出群聊
        String name = resolveDisplayName(userRepository.findById(userId).orElse(null));
        String sysMsg = name + "已被移出群聊";
        publishGroupSystemMessage(groupId, userId, sysMsg);
        // 保留被踢用户的会话，只更新预览（publishGroupSystemMessage 已更新所有现有成员，这里补充被踢用户）
        jdbc.update("INSERT INTO conversation (user_id, target_type, target_id, last_message_preview, last_message_at) VALUES (?,2,?,?,NOW()) ON DUPLICATE KEY UPDATE last_message_preview=?, last_message_at=NOW()",
                userId, groupId, sysMsg, sysMsg);
    }

    @Transactional
    public void dismissGroup(Long operatorId, Long groupId) {
        ChatGroup group = getGroup(groupId);

        if (!group.getOwnerId().equals(operatorId)) {
            throw new BusinessException("只有群主可以解散群组");
        }

        groupMemberRepository.deleteByGroupId(groupId);
        chatGroupRepository.delete(group);
    }

    @Transactional
    public void transferOwner(Long operatorId, Long groupId, Long newOwnerId) {
        ChatGroup group = getGroup(groupId);

        if (!group.getOwnerId().equals(operatorId)) {
            throw new BusinessException("只有群主可以转让");
        }

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, newOwnerId)) {
            throw new BusinessException("新群主必须是群成员");
        }

        group.setOwnerId(newOwnerId);
        chatGroupRepository.save(group);

        GroupMember newOwner = groupMemberRepository.findByGroupIdAndUserId(groupId, newOwnerId).get();
        newOwner.setRole(Constants.GROUP_ROLE_OWNER);
        groupMemberRepository.save(newOwner);

        GroupMember oldOwner = groupMemberRepository.findByGroupIdAndUserId(groupId, operatorId).get();
        oldOwner.setRole(Constants.GROUP_ROLE_MEMBER);
        groupMemberRepository.save(oldOwner);
    }

    @Transactional
    public void updateMemberNickname(Long userId, Long groupId, String nickname) {
        ensureMember(groupId, userId);
        groupMemberRepository.updateNickname(groupId, userId, nickname);
    }

    @Transactional
    public void updateMyGroupRemark(Long userId, Long groupId, String remark) {
        ensureMember(groupId, userId);
        groupMemberRepository.updateMyGroupRemark(groupId, userId, remark);
    }

    @Transactional
    public void updateNotice(Long userId, Long groupId, String notice) {
        ChatGroup group = getGroup(groupId);
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException("只有群主可以修改群公告");
        }
        group.setNotice(notice);
        group.setNoticeUpdatedAt(LocalDateTime.now());
        chatGroupRepository.save(group);
    }

    /**
     * 设置/取消管理员
     */
    @Transactional
    public void setAdmin(Long operatorId, Long groupId, Long userId, boolean isAdmin) {
        ChatGroup group = getGroup(groupId);
        if (!group.getOwnerId().equals(operatorId)) {
            throw new BusinessException("只有群主可以设置管理员");
        }
        if (group.getOwnerId().equals(userId)) {
            throw new BusinessException("不能修改群主的角色");
        }
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException("用户不在群中"));
        member.setRole(isAdmin ? Constants.GROUP_ROLE_ADMIN : Constants.GROUP_ROLE_MEMBER);
        groupMemberRepository.save(member);
    }

    /**
     * 禁言/取消禁言
     */
    @Transactional
    public void muteMember(Long operatorId, Long groupId, Long userId, boolean mute) {
        ChatGroup group = getGroup(groupId);
        GroupMember operator = groupMemberRepository.findByGroupIdAndUserId(groupId, operatorId)
                .orElseThrow(() -> new BusinessException("不是群成员"));
        if (operator.getRole() > Constants.GROUP_ROLE_ADMIN) {
            throw new BusinessException("没有权限禁言");
        }
        if (group.getOwnerId().equals(userId)) {
            throw new BusinessException("不能禁言群主");
        }
        GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException("用户不在群中"));
        target.setIsMuted(mute ? 1 : 0);
        target.setMutedUntil(mute ? LocalDateTime.now().plusDays(365) : null);
        groupMemberRepository.save(target);
    }

    // 消息免打扰（自己给自己设，不影响禁言状态）
    @Transactional
    public void muteNotification(Long userId, Long groupId, boolean mute) {
        GroupMember self = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException("不是群成员"));
        self.setIsNotificationMuted(mute ? 1 : 0);
        groupMemberRepository.save(self);
    }

    /**
     * 检查用户是否被禁言
     */
    public boolean isMuted(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .map(m -> m.getIsMuted() != null && m.getIsMuted() == 1)
                .orElse(false);
    }

    /**
     * 上传群文件
     */
    @Transactional
    public GroupFileVO uploadGroupFile(Long userId, Long groupId, Long fileId) {
        ensureMember(groupId, userId);
        StoredFile file = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));

        GroupFile gf = new GroupFile();
        gf.setGroupId(groupId);
        gf.setUploaderId(userId);
        gf.setFileId(fileId);
        groupFileRepository.save(gf);

        var uploader = userService.getById(userId);
        return GroupFileVO.builder()
                .id(gf.getId())
                .groupId(groupId)
                .uploaderId(userId)
                .uploaderName(uploader.getNickname() != null ? uploader.getNickname() : uploader.getUsername())
                .fileId(fileId)
                .originalName(file.getOriginalName())
                .url(file.getStoredPath())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .createdAt(gf.getCreatedAt())
                .build();
    }

    /**
     * 查看群文件列表（批量加载避免 N+1）
     */
    public List<GroupFileVO> listGroupFiles(Long userId, Long groupId) {
        ensureMember(groupId, userId);
        List<GroupFile> groupFiles = groupFileRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        if (groupFiles.isEmpty()) return List.of();
        // 批量加载关联文件
        List<Long> fileIds = groupFiles.stream().map(GroupFile::getFileId).distinct().toList();
        Map<Long, StoredFile> fileMap = storedFileRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(StoredFile::getId, Function.identity()));
        // 批量加载上传者
        List<Long> uploaderIds = groupFiles.stream().map(GroupFile::getUploaderId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return groupFiles.stream()
                .map(gf -> {
                    var file = fileMap.get(gf.getFileId());
                    if (file == null) return null;
                    var uploader = userMap.get(gf.getUploaderId());
                    String uploaderName = uploader != null
                            ? (uploader.getNickname() != null ? uploader.getNickname() : uploader.getUsername())
                            : "未知用户";
                    return GroupFileVO.builder()
                            .id(gf.getId())
                            .groupId(groupId)
                            .uploaderId(gf.getUploaderId())
                            .uploaderName(uploaderName)
                            .fileId(file.getId())
                            .originalName(file.getOriginalName())
                            .url(file.getStoredPath())
                            .fileSize(file.getFileSize())
                            .mimeType(file.getMimeType())
                            .createdAt(gf.getCreatedAt())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public List<GroupVO> searchGroups(String keyword) {
        // 先按群号精确搜，再按名称模糊搜
        var byCode = chatGroupRepository.findByGroupCode(keyword);
        if (byCode.isPresent()) {
            return List.of(getGroupDetail(byCode.get().getId()));
        }
        List<ChatGroup> groups = chatGroupRepository.searchByName(keyword);
        return groups.stream()
                .map(group -> getGroupDetail(group.getId()))
                .toList();
    }

    public GroupVO getGroupByCode(String code) {
        ChatGroup g = chatGroupRepository.findByGroupCode(code)
                .orElseThrow(() -> new BusinessException("群聊不存在"));
        return getGroupDetail(g.getId());
    }

    @Transactional
    public void joinByGroupCode(Long userId, String groupCode) {
        ChatGroup group = chatGroupRepository.findByGroupCode(groupCode)
                .orElseThrow(() -> new BusinessException("群聊不存在"));
        joinGroup(userId, group.getId());
    }

    @Transactional
    public void joinGroup(Long userId, Long groupId) {
        ChatGroup group = getGroup(groupId);
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new BusinessException("已在群中");
        }
        int mode = group.getJoinMode() != null ? group.getJoinMode() : 0;
        if (mode == 2) throw new BusinessException("该群禁止加入");
        if (mode == 1) {
            // 独立连接写入，避開当前事务回滚
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().executeUpdate(
                    "INSERT INTO group_join_request (group_id, user_id, status) VALUES (" + groupId + "," + userId + ",0) ON DUPLICATE KEY UPDATE status=0, created_at=NOW()");
            } catch (Exception ignored) {}
            eventPublisher.publishEvent(new JoinRequestEvent(groupId, group.getOwnerId()));
            throw new BusinessException("已发送入群申请，等待群主审批");
        }
        userService.requireUser(userId);
        saveMember(groupId, userId, Constants.GROUP_ROLE_MEMBER);
    }

    @Transactional
    public void setJoinMode(Long userId, Long groupId, Integer mode) {
        ChatGroup group = getGroup(groupId);
        if (!group.getOwnerId().equals(userId)) throw new BusinessException("只有群主可以设置");
        group.setJoinMode(mode);
        chatGroupRepository.save(group);
    }

    @Transactional
    public GroupVO updateName(Long userId, Long groupId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BusinessException("群名称不能为空");
        }
        ChatGroup group = getGroup(groupId);
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException("不是群成员"));
        // 群主或管理员才能改群名
        if (member.getRole() > Constants.GROUP_ROLE_ADMIN) {
            throw new BusinessException("只有群主和管理员可以修改群名称");
        }
        group.setName(newName.trim());
        chatGroupRepository.save(group);
        return getGroupDetail(groupId, userId);
    }

    public Map<Long, Long> getPendingDetail(Long userId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT r.group_id, COUNT(*) as cnt FROM group_join_request r JOIN `group` g ON r.group_id=g.id WHERE g.owner_id=? AND r.status=0 GROUP BY r.group_id", userId);
            Map<Long, Long> result = new HashMap<>();
            for (var row : rows) result.put(((Number) row.get("group_id")).longValue(), ((Number) row.get("cnt")).longValue());
            return result;
        } catch (Exception e) { return Map.of(); }
    }


    public int getPendingRequestCount(Long userId) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM group_join_request r JOIN `group` g ON r.group_id=g.id WHERE g.owner_id=? AND r.status=0",
            Integer.class, userId);
    }

    public List<Map<String, Object>> getPendingRequests(Long userId, Long groupId) {
        ChatGroup g = getGroup(groupId);
        if (!g.getOwnerId().equals(userId)) throw new BusinessException("仅群主可查看");
        return jdbc.queryForList(
            "SELECT r.id, r.user_id, u.nickname, u.username, u.avatar, r.created_at FROM group_join_request r JOIN user u ON r.user_id=u.id WHERE r.group_id=? AND r.status=0", groupId);
    }

    @Transactional
    public void approveRequest(Long userId, Long groupId, Long requestId, boolean approve) {
        ChatGroup g = getGroup(groupId);
        if (!g.getOwnerId().equals(userId)) throw new BusinessException("仅群主可操作");
        var rows = jdbc.queryForList("SELECT * FROM group_join_request WHERE id=? AND status=0", requestId);
        if (rows.isEmpty()) throw new BusinessException("申请不存在");
        Long applicantId = (Long) rows.get(0).get("user_id");
        jdbc.update("UPDATE group_join_request SET status=? WHERE id=?", approve ? 1 : 2, requestId);
        if (approve && !groupMemberRepository.existsByGroupIdAndUserId(groupId, applicantId)) {
            saveMember(groupId, applicantId, Constants.GROUP_ROLE_MEMBER);
            String name = resolveDisplayName(userRepository.findById(applicantId).orElse(null));
            String sysMsg = name + "加入了群聊";
            publishGroupSystemMessage(groupId, applicantId, sysMsg);
        }
    }

    @Transactional
    public GroupVO updateAvatar(Long userId, Long groupId, String avatarUrl) {
        ChatGroup group = getGroup(groupId);
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException("不是群成员"));
        if (member.getRole() > Constants.GROUP_ROLE_ADMIN) {
            throw new BusinessException("仅群主和管理员可修改");
        }
        group.setAvatar(avatarUrl);
        chatGroupRepository.save(group);
        return getGroupDetail(groupId);
    }
}
