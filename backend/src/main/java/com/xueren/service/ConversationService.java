package com.xueren.service;

import com.xueren.common.BusinessException;
import com.xueren.common.Constants;
import com.xueren.dto.ConversationVO;
import com.xueren.entity.Conversation;
import com.xueren.entity.Message;
import com.xueren.netty.ChannelManager;
import com.xueren.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserService userService;
    private final GroupService groupService;
    private final ChannelManager channelManager;

    public ConversationService(ConversationRepository conversationRepository,
                               UserService userService,
                               GroupService groupService,
                               ChannelManager channelManager) {
        this.conversationRepository = conversationRepository;
        this.userService = userService;
        this.groupService = groupService;
        this.channelManager = channelManager;
    }

    public List<ConversationVO> listConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByLastMessageAtDesc(userId)
                .stream()
                .map(this::toVO)
                .toList();
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

        conversation.setLastMessageId(message.getId());
        conversation.setLastMessagePreview(buildPreview(message));
        conversation.setLastMessageAt(message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now());
        if (increaseUnread) {
            conversation.setUnreadCount((conversation.getUnreadCount() == null ? 0 : conversation.getUnreadCount()) + 1);
        }
        conversationRepository.save(conversation);
    }

    private ConversationVO toVO(Conversation conversation) {
        String name;
        String avatar;
        Boolean isOnline = false;
        if (conversation.getTargetType() == Constants.TARGET_USER) {
            var user = userService.getById(conversation.getTargetId());
            name = user.getNickname() != null && !user.getNickname().isEmpty() ? user.getNickname() : user.getUsername();
            avatar = user.getAvatar();
            isOnline = channelManager.isOnline(conversation.getTargetId());
        } else {
            var group = groupService.getGroup(conversation.getTargetId());
            name = group.getName();
            avatar = group.getAvatar();
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
                .targetIsOnline(isOnline)
                .build();
    }

    private String buildPreview(Message message) {
        if (message.getIsRecalled() != null && message.getIsRecalled() == 1) {
            return "[消息已撤回]";
        }
        return switch (message.getMsgType()) {
            case Constants.MSG_IMAGE -> "[图片]";
            case Constants.MSG_FILE -> "[文件]";
            default -> message.getContent() != null && message.getContent().length() > 200
                    ? message.getContent().substring(0, 200)
                    : message.getContent();
        };
    }
}
