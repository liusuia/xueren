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
import com.xueren.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ChannelManager channelManager;
    private final JdbcTemplate jdbc;

    public FriendService(FriendRepository friendRepository, UserService userService,
                        UserRepository userRepository,
                        ConversationRepository conversationRepository, ChannelManager channelManager,
                        JdbcTemplate jdbc) {
        this.friendRepository = friendRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.channelManager = channelManager;
        this.jdbc = jdbc;
    }

    @Transactional
    public void sendRequest(Long userId, Long friendId, String verificationMsg) {
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
        request.setVerificationMsg(verificationMsg);
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
        // 发送系统消息：你们已成为好友
        sendFriendAcceptedMessage(userId, requesterId);

        Friend reverse = friendRepository.findByUserIdAndFriendId(userId, requesterId).orElse(null);
        if (reverse == null) {
            reverse = new Friend();
            reverse.setUserId(userId);
            reverse.setFriendId(requesterId);
            reverse.setRequesterId(requesterId);
            reverse.setStatus(Constants.FRIEND_ACCEPTED);
            try {
                friendRepository.save(reverse);
            } catch (DataIntegrityViolationException e) {
                // 并发创建被唯一约束拦截，另一条记录已生效
            }
        } else {
            reverse.setStatus(Constants.FRIEND_ACCEPTED);
            friendRepository.save(reverse);
        }
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
        List<Friend> friends = friendRepository.findByUserIdAndStatus(userId, Constants.FRIEND_ACCEPTED);
        // 批量加载好友用户
        List<Long> friendIds = friends.stream().map(Friend::getFriendId).distinct().toList();
        Map<Long, User> userMap = friendIds.isEmpty() ? Map.of()
                : userRepository.findAllById(friendIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return friends.stream().map(f -> toVO(f, userMap.get(f.getFriendId()))).toList();
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
        List<Friend> requests = friendRepository.findByFriendIdAndStatus(userId, Constants.FRIEND_PENDING);
        List<Friend> valid = requests.stream().filter(f -> f.getRequesterId().equals(f.getUserId())).toList();
        // 批量加载申请者用户
        List<Long> requesterIds = valid.stream().map(Friend::getUserId).distinct().toList();
        Map<Long, User> userMap = requesterIds.isEmpty() ? Map.of()
                : userRepository.findAllById(requesterIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return valid.stream().map(f -> toVO(f, userMap.get(f.getUserId()))).toList();
    }

    private void sendFriendAcceptedMessage(Long userA, Long userB) {
        try {
            String msg = "你们已成为好友，现在可以开始聊天了";
            jdbc.update("INSERT INTO message (chat_type,from_user_id,to_user_id,content,msg_type,created_at) VALUES (?,?,?,?,?,NOW())", 1, userA, userB, msg, 1);
            jdbc.update("INSERT INTO message (chat_type,from_user_id,to_user_id,content,msg_type,created_at) VALUES (?,?,?,?,?,NOW())", 1, userB, userA, msg, 1);
            // 创建/更新双方的会话
            jdbc.update("INSERT INTO conversation (user_id,target_type,target_id,last_message_preview,last_message_at) VALUES (?,1,?,?,NOW()) ON DUPLICATE KEY UPDATE last_message_preview=?,last_message_at=NOW()", userA, userB, msg, msg);
            jdbc.update("INSERT INTO conversation (user_id,target_type,target_id,last_message_preview,last_message_at) VALUES (?,1,?,?,NOW()) ON DUPLICATE KEY UPDATE last_message_preview=?,last_message_at=NOW()", userB, userA, msg, msg);
        } catch (Exception ignored) {}
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
                .verificationMsg(friend.getVerificationMsg())
                .status(friend.getStatus())
                .requesterId(friend.getRequesterId())
                .createdAt(friend.getCreatedAt())
                .isOnline(channelManager.isOnline(user.getId()))
                .build();
    }
}
