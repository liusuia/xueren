package com.xueren.service;

import com.xueren.common.BusinessException;
import com.xueren.common.Constants;
import com.xueren.dto.ConversationVO;
import com.xueren.entity.ChatGroup;
import com.xueren.entity.Conversation;
import com.xueren.entity.Message;
import com.xueren.entity.User;
import com.xueren.netty.ChannelManager;
import com.xueren.repository.ChatGroupRepository;
import com.xueren.repository.ConversationRepository;
import com.xueren.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserService userService;
    private final GroupService groupService;
    private final ChannelManager channelManager;
    private final UserRepository userRepository;
    private final ChatGroupRepository chatGroupRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               UserService userService,
                               GroupService groupService,
                               ChannelManager channelManager,
                               UserRepository userRepository,
                               ChatGroupRepository chatGroupRepository) {
        this.conversationRepository = conversationRepository;
        this.userService = userService;
        this.groupService = groupService;
        this.channelManager = channelManager;
        this.userRepository = userRepository;
        this.chatGroupRepository = chatGroupRepository;
    }

    public List<ConversationVO> listConversations(Long userId) {
        List<Conversation> convs = conversationRepository.findByUserIdOrderByLastMessageAtDesc(userId);
        // 批量加载用户和群组，替代 N+1
        List<Long> userIds = convs.stream().filter(c -> c.getTargetType() == Constants.TARGET_USER).map(Conversation::getTargetId).distinct().toList();
        List<Long> groupIds = convs.stream().filter(c -> c.getTargetType() == Constants.TARGET_GROUP).map(Conversation::getTargetId).distinct().toList();
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() : userRepository.findAllById(userIds).stream().collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
        Map<Long, ChatGroup> groupMap = groupIds.isEmpty() ? Map.of() : chatGroupRepository.findAllById(groupIds).stream().collect(java.util.stream.Collectors.toMap(ChatGroup::getId, g -> g));
        return convs.stream()
                .map(c -> toVO(c, userMap, groupMap))
                .toList();
    }

    private ConversationVO toVO(Conversation conversation, Map<Long, User> userMap, Map<Long, ChatGroup> groupMap) {
        String name;
        String avatar;
        Boolean isOnline = false;
        if (conversation.getTargetType() == Constants.TARGET_USER) {
            var user = userMap.get(conversation.getTargetId());
            name = user != null ? (user.getNickname() != null && !user.getNickname().isEmpty() ? user.getNickname() : user.getUsername()) : "";
            avatar = user != null ? user.getAvatar() : null;
            isOnline = channelManager.isOnline(conversation.getTargetId());
        } else {
            var group = groupMap.get(conversation.getTargetId());
            name = group != null ? group.getName() : "";
            avatar = group != null ? group.getAvatar() : null;
        }
        return ConversationVO.builder()
                .id(conversation.getId())
                .targetType(conversation.getTargetType())
                .targetId(conversation.getTargetId())
                .targetName(name)
                .targetAvatar(avatar)
                .lastMessagePreview(conversation.getLastMessagePreview())
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(conversation.getUnreadCount())
                .draft(conversation.getDraft())
                .targetIsOnline(isOnline)
                .build();
    }

    @Transactional
    public void touchSingleConversation(Long userId, Long peerId, Message message) {
        touchConversation(userId, Constants.TARGET_USER, peerId, message, false);
        touchConversation(peerId, Constants.TARGET_USER, userId, message, true);
    }

    @Transactional
    public void touchGroupConversation(Long groupId, Message message, Long senderId) {
        groupService.listMemberUserIds(groupId).forEach(memberId -> {
            boolean increaseUnread = !memberId.equals(senderId);
            touchConversation(memberId, Constants.TARGET_GROUP, groupId, message, increaseUnread);
        });
    }

    @Transactional
    public void markRead(Long userId, Integer targetType, Long targetId, Long lastMessageId) {
        Conversation conversation = conversationRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .orElse(null);
        if (conversation == null) {
            return;
        }
        conversation.setUnreadCount(0);
        conversation.setLastReadMessageId(lastMessageId);
        conversationRepository.save(conversation);
    }

    @Transactional
    public void saveDraft(Long userId, Integer targetType, Long targetId, String draft) {
        Conversation conv = conversationRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setUserId(userId);
                    c.setTargetType(targetType);
                    c.setTargetId(targetId);
                    c.setUnreadCount(0);
                    return c;
                });
        conv.setDraft(draft != null && draft.isBlank() ? null : draft);
        conversationRepository.save(conv);
    }

    @Transactional
    public boolean togglePin(Long userId, Integer targetType, Long targetId) {
        Conversation conv = conversationRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setUserId(userId);
                    c.setTargetType(targetType);
                    c.setTargetId(targetId);
                    c.setUnreadCount(0);
                    c.setIsPinned(0);
                    return c;
                });
        boolean newPinned = conv.getIsPinned() == null || conv.getIsPinned() == 0;
        conv.setIsPinned(newPinned ? 1 : 0);
        conversationRepository.save(conv);
        return newPinned;
    }

    @Transactional
    public void deleteConversation(Long userId, Long convId) {
        Conversation conversation = conversationRepository.findById(convId)
                .orElseThrow(() -> new BusinessException("会话不存在"));
        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该会话");
        }
        conversationRepository.delete(conversation);
    }

    private void touchConversation(Long userId,
                                   Integer targetType,
                                   Long targetId,
                                   Message message,
                                   boolean increaseUnread) {
        Conversation conversation = conversationRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setUserId(userId);
                    c.setTargetType(targetType);
                    c.setTargetId(targetId);
                    c.setUnreadCount(0);
                    return c;
                });

        boolean isNew = conversation.getId() == null;
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessagePreview(buildPreview(message));
        conversation.setLastMessageAt(message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now());

        if (isNew) {
            conversation.setUnreadCount(increaseUnread ? 1 : 0);
            if (!increaseUnread) conversation.setLastReadMessageId(message.getId());
            conversationRepository.save(conversation);
        } else if (increaseUnread) {
            // 已存在会话：用 updateMeta 更新元信息（不覆盖 unreadCount），再原子递增加1
            conversationRepository.updateMeta(conversation.getId(), message.getId(), buildPreview(message),
                    message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now());
            conversationRepository.incrementUnread(conversation.getId());
        } else {
            conversation.setUnreadCount(0);
            conversation.setLastReadMessageId(message.getId());
            conversationRepository.save(conversation);
        }
    }

    private String buildPreview(Message message) {
        if (message.getIsRecalled() != null && message.getIsRecalled() == 1) {
            return "[消息已撤回]";
        }
        String sender = "";
        if (message.getChatType() != null && message.getChatType() == Constants.CHAT_GROUP) {
            User fromUser = userRepository.findById(message.getFromUserId()).orElse(null);
            if (fromUser != null) {
                sender = (fromUser.getNickname() != null ? fromUser.getNickname() : fromUser.getUsername()) + ": ";
            }
        }
        String content = switch (message.getMsgType()) {
            case Constants.MSG_IMAGE -> "[图片]";
            case Constants.MSG_STICKER -> "[表情]";
            case Constants.MSG_FILE -> "[文件]";
            case Constants.MSG_CONTACT_CARD -> "[名片]";
            default -> message.getContent() != null && message.getContent().length() > 200
                    ? message.getContent().substring(0, 200)
                    : message.getContent();
        };
        return (sender + content).length() > 200 ? (sender + content).substring(0, 200) : sender + content;
    }
}
