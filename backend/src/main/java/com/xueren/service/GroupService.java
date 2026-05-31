package com.xueren.service;

import com.xueren.common.BusinessException;
import com.xueren.common.Constants;
import com.xueren.dto.CreateGroupRequest;
import com.xueren.dto.GroupFileVO;
import com.xueren.dto.GroupMemberVO;
import com.xueren.dto.GroupVO;
import com.xueren.entity.ChatGroup;
import com.xueren.entity.GroupFile;
import com.xueren.entity.GroupMember;
import com.xueren.entity.StoredFile;
import com.xueren.repository.ChatGroupRepository;
import com.xueren.repository.GroupFileRepository;
import com.xueren.repository.GroupMemberRepository;
import com.xueren.repository.StoredFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GroupService {

    private final ChatGroupRepository chatGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupFileRepository groupFileRepository;
    private final StoredFileRepository storedFileRepository;
    private final UserService userService;

    public GroupService(ChatGroupRepository chatGroupRepository,
                        GroupMemberRepository groupMemberRepository,
                        GroupFileRepository groupFileRepository,
                        StoredFileRepository storedFileRepository,
                        UserService userService) {
        this.chatGroupRepository = chatGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupFileRepository = groupFileRepository;
        this.storedFileRepository = storedFileRepository;
        this.userService = userService;
    }

    @Transactional
    public GroupVO createGroup(Long ownerId, CreateGroupRequest request) {
        ChatGroup group = new ChatGroup();
        group.setName(request.getName());
        group.setOwnerId(ownerId);
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
        String remark = null;
        if (userId != null) {
            remark = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                    .map(GroupMember::getRemark).orElse(null);
        }
        return GroupVO.builder()
                .id(group.getId())
                .name(group.getName())
                .avatar(group.getAvatar())
                .ownerId(group.getOwnerId())
                .createdAt(group.getCreatedAt())
                .remark(remark)
                .notice(group.getNotice())
                .noticeUpdatedAt(group.getNoticeUpdatedAt())
                .members(members.stream()
                        .map(m -> userService.getById(m.getUserId()))
                        .toList())
                .memberVOs(members.stream()
                        .map(m -> {
                            var user = userService.getById(m.getUserId());
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
                                    .build();
                        })
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
        for (Long userId : userIds) {
            if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
                userService.requireUser(userId);
                saveMember(groupId, userId, Constants.GROUP_ROLE_MEMBER);
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
     * 查看群文件列表
     */
    public List<GroupFileVO> listGroupFiles(Long userId, Long groupId) {
        ensureMember(groupId, userId);
        return groupFileRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(gf -> {
                    var file = storedFileRepository.findById(gf.getFileId()).orElse(null);
                    var uploader = userService.getById(gf.getUploaderId());
                    if (file == null) return null;
                    return GroupFileVO.builder()
                            .id(gf.getId())
                            .groupId(groupId)
                            .uploaderId(gf.getUploaderId())
                            .uploaderName(uploader.getNickname() != null ? uploader.getNickname() : uploader.getUsername())
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
        List<ChatGroup> groups = chatGroupRepository.searchByName(keyword);
        return groups.stream()
                .map(group -> getGroupDetail(group.getId()))
                .toList();
    }

    @Transactional
    public void joinGroup(Long userId, Long groupId) {
        ChatGroup group = getGroup(groupId);

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new BusinessException("已在群中");
        }

        userService.requireUser(userId);

        saveMember(groupId, userId, Constants.GROUP_ROLE_MEMBER);
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

    @Transactional
    public GroupVO updateAvatar(Long userId, Long groupId, String avatarUrl) {
        ChatGroup group = getGroup(groupId);
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException("只有群主可以修改群头像");
        }
        group.setAvatar(avatarUrl);
        chatGroupRepository.save(group);
        return getGroupDetail(groupId);
    }
}
