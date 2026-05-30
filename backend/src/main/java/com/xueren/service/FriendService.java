package com.xueren.service;

import com.xueren.common.BusinessException;
import com.xueren.common.Constants;
import com.xueren.dto.FriendVO;
import com.xueren.dto.RemarkRequest;
import com.xueren.entity.Friend;
import com.xueren.entity.User;
import com.xueren.netty.ChannelManager;
import com.xueren.repository.ConversationRepository;
import com.xueren.repository.FriendRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserService userService;
    private final ConversationRepository conversationRepository;
    private final ChannelManager channelManager;

    public FriendService(FriendRepository friendRepository, UserService userService,
                        ConversationRepository conversationRepository, ChannelManager channelManager) {
        this.friendRepository = friendRepository;
        this.userService = userService;
        this.conversationRepository = conversationRepository;
        this.channelManager = channelManager;
    }

    @Transactional
    public void sendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new BusinessException("不能添加自己为好友");
        }
        userService.requireUser(friendId);

        Friend existing = friendRepository.findByUserIdAndFriendId(userId, friendId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == Constants.FRIEND_ACCEPTED) {
                throw new BusinessException("已经是好友");
            }
            if (existing.getStatus() == Constants.FRIEND_PENDING) {
                throw new BusinessException("已发送好友申请");
            }
            if (existing.getStatus() == Constants.FRIEND_BLOCKED) {
                throw new BusinessException("对方已被拉黑");
            }
        }

        Friend reverse = friendRepository.findByUserIdAndFriendId(friendId, userId).orElse(null);
        if (reverse != null && reverse.getStatus() == Constants.FRIEND_BLOCKED) {
            throw new BusinessException("无法添加该用户");
        }

        Friend request = new Friend();
        request.setUserId(userId);
        request.setFriendId(friendId);
        request.setRequesterId(userId);
        request.setStatus(Constants.FRIEND_PENDING);
        friendRepository.save(request);
    }

    @Transactional
    public void acceptRequest(Long userId, Long requesterId) {
        Friend incoming = friendRepository.findByUserIdAndFriendId(requesterId, userId)
                .orElseThrow(() -> new BusinessException("好友申请不存在"));
        if (incoming.getStatus() != Constants.FRIEND_PENDING || !incoming.getRequesterId().equals(requesterId)) {
            throw new BusinessException("好友申请状态无效");
        }
        incoming.setStatus(Constants.FRIEND_ACCEPTED);
        friendRepository.save(incoming);

        Friend reverse = friendRepository.findByUserIdAndFriendId(userId, requesterId).orElse(null);
        if (reverse == null) {
            reverse = new Friend();
            reverse.setUserId(userId);
            reverse.setFriendId(requesterId);
            reverse.setRequesterId(requesterId);
        }
        reverse.setStatus(Constants.FRIEND_ACCEPTED);
        friendRepository.save(reverse);
    }

    @Transactional
    public void rejectRequest(Long userId, Long requesterId) {
        Friend incoming = friendRepository.findByUserIdAndFriendId(requesterId, userId)
                .orElseThrow(() -> new BusinessException("好友申请不存在"));
        incoming.setStatus(Constants.FRIEND_REJECTED);
        friendRepository.save(incoming);
    }

    @Transactional
    public void blockFriend(Long userId, Long friendId) {
        Friend relation = friendRepository.findByUserIdAndFriendId(userId, friendId).orElseGet(() -> {
            Friend f = new Friend();
            f.setUserId(userId);
            f.setFriendId(friendId);
            f.setRequesterId(userId);
            return f;
        });
        relation.setStatus(Constants.FRIEND_BLOCKED);
        friendRepository.save(relation);
    }

    @Transactional
    public void unblockFriend(Long userId, Long friendId) {
        Friend relation = friendRepository.findByUserIdAndFriendId(userId, friendId)
                .orElseThrow(() -> new BusinessException("拉黑关系不存在"));
        if (relation.getStatus() != Constants.FRIEND_BLOCKED) {
            throw new BusinessException("该用户未被拉黑");
        }
        // 恢复为好友状态；如果没有好友关系则删除记录
        Friend reverse = friendRepository.findByUserIdAndFriendId(friendId, userId).orElse(null);
        if (reverse != null && reverse.getStatus() == Constants.FRIEND_ACCEPTED) {
            relation.setStatus(Constants.FRIEND_ACCEPTED);
            friendRepository.save(relation);
        } else {
            friendRepository.delete(relation);
        }
    }

    @Transactional
    public void updateRemark(Long userId, Long friendId, RemarkRequest request) {
        Friend relation = friendRepository.findByUserIdAndFriendId(userId, friendId)
                .orElseThrow(() -> new BusinessException("好友关系不存在"));
        if (relation.getStatus() != Constants.FRIEND_ACCEPTED) {
            throw new BusinessException("只能给好友设置备注");
        }
        relation.setRemark(request.getRemark());
        friendRepository.save(relation);
    }

    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        Friend relation = friendRepository.findByUserIdAndFriendId(userId, friendId)
                .orElseThrow(() -> new BusinessException("好友关系不存在"));
        friendRepository.delete(relation);

        // 删除反向关系
        Friend reverse = friendRepository.findByUserIdAndFriendId(friendId, userId).orElse(null);
        if (reverse != null) {
            friendRepository.delete(reverse);
        }

        // 删除双方的会话记录
        conversationRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, Constants.TARGET_USER, friendId);
        conversationRepository.deleteByUserIdAndTargetTypeAndTargetId(friendId, Constants.TARGET_USER, userId);
    }

    public List<FriendVO> listFriends(Long userId) {
        return friendRepository.findByUserIdAndStatus(userId, Constants.FRIEND_ACCEPTED)
                .stream()
                .map(f -> toVO(f, userService.requireUser(f.getFriendId())))
                .toList();
    }

    public List<Long> listBlockedUserIds(Long userId) {
        return friendRepository.findByUserIdAndStatus(userId, Constants.FRIEND_BLOCKED)
                .stream()
                .map(Friend::getFriendId)
                .toList();
    }

    public List<Long> listBlockedByUserIds(Long userId) {
        return friendRepository.findByFriendIdAndStatus(userId, Constants.FRIEND_BLOCKED)
                .stream()
                .map(Friend::getUserId)
                .toList();
    }

    public List<FriendVO> listIncomingRequests(Long userId) {
        List<FriendVO> result = new ArrayList<>();
        for (Friend f : friendRepository.findByFriendIdAndStatus(userId, Constants.FRIEND_PENDING)) {
            if (f.getRequesterId().equals(f.getUserId())) {
                result.add(toVO(f, userService.requireUser(f.getUserId())));
            }
        }
        return result;
    }

    public void ensureFriend(Long userId, Long peerId) {
        if (userId.equals(peerId)) {
            return;
        }
        // 检查对方是否拉黑了我
        Friend blockedByThem = friendRepository.findByUserIdAndFriendId(peerId, userId).orElse(null);
        if (blockedByThem != null && blockedByThem.getStatus() == Constants.FRIEND_BLOCKED) {
            throw new BusinessException("你已被对方拉黑");
        }

        Friend relation = friendRepository.findByUserIdAndFriendId(userId, peerId)
                .orElseThrow(() -> new BusinessException("不是好友，无法发送消息"));
        if (relation.getStatus() == Constants.FRIEND_BLOCKED) {
            throw new BusinessException("对方已被你拉黑");
        }
        if (relation.getStatus() != Constants.FRIEND_ACCEPTED) {
            throw new BusinessException("不是好友，无法发送消息");
        }
    }

    private FriendVO toVO(Friend friend, User user) {
        return FriendVO.builder()
                .id(friend.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .remark(friend.getRemark())
                .status(friend.getStatus())
                .requesterId(friend.getRequesterId())
                .createdAt(friend.getCreatedAt())
                .isOnline(channelManager.isOnline(user.getId()))
                .build();
    }
}
