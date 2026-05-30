package com.xueren.service;

import com.xueren.common.BusinessException;
import com.xueren.common.Constants;
import com.xueren.dto.MessageVO;
import com.xueren.dto.SendMessageRequest;
import com.xueren.entity.Message;
import com.xueren.entity.MessageRead;
import com.xueren.entity.StoredFile;
import com.xueren.entity.User;
import com.xueren.repository.ConversationRepository;
import com.xueren.repository.MessageReadRepository;
import com.xueren.repository.MessageRepository;
import com.xueren.repository.StoredFileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageReadRepository messageReadRepository;
    private final StoredFileRepository storedFileRepository;
    private final ConversationRepository conversationRepository;
    private final GroupService groupService;
    private final FriendService friendService;
    private final ConversationService conversationService;
    private final UserService userService;
    private final MessagePushService messagePushService;

    public MessageService(MessageRepository messageRepository,
                          MessageReadRepository messageReadRepository,
                          StoredFileRepository storedFileRepository,
                          ConversationRepository conversationRepository,
                          FriendService friendService,
                          GroupService groupService,
                          ConversationService conversationService,
                          UserService userService,
                          MessagePushService messagePushService) {
        this.messageRepository = messageRepository;
        this.messageReadRepository = messageReadRepository;
        this.storedFileRepository = storedFileRepository;
        this.conversationRepository = conversationRepository;
        this.friendService = friendService;
        this.groupService = groupService;
        this.conversationService = conversationService;
        this.userService = userService;
        this.messagePushService = messagePushService;
    }

    @Transactional
    public MessageVO send(Long userId, SendMessageRequest request) {
        validateSendRequest(request);

        // 检查群聊禁言
        if (request.getChatType() == Constants.CHAT_GROUP) {
            if (groupService.isMuted(request.getGroupId(), userId)) {
                throw new BusinessException("你已被禁言");
            }
        }

        Message message = new Message();
        message.setChatType(request.getChatType());
        message.setFromUserId(userId);
        message.setMsgType(request.getMsgType());
        message.setFileId(request.getFileId());
        message.setIsRecalled(0);

        // 存储@提及的用户ID
        if (request.getMentionedUserIds() != null && !request.getMentionedUserIds().isEmpty()) {
            message.setMentionUserIds(request.getMentionedUserIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }

        if (request.getChatType() == Constants.CHAT_SINGLE) {
            friendService.ensureFriend(userId, request.getToUserId());
            message.setToUserId(request.getToUserId());
            message.setContent(resolveContent(request));
            messageRepository.save(message);
            conversationService.touchSingleConversation(userId, request.getToUserId(), message);
        } else {
            groupService.ensureMember(request.getGroupId(), userId);
            message.setGroupId(request.getGroupId());
            message.setContent(resolveContent(request));
            messageRepository.save(message);
            conversationService.touchGroupConversation(request.getGroupId(), message, userId);
        }
        MessageVO vo = toVO(message);
        messagePushService.pushNewMessage(vo);
        return vo;
    }

    public List<MessageVO> listSingleChat(Long userId, Long peerId, int limit) {
        List<Message> messages = messageRepository.findSingleChat(userId, peerId, PageRequest.of(0, limit));
        Collections.reverse(messages);
        return messages.stream().map(this::toVO).toList();
    }

    public List<MessageVO> listGroupChat(Long userId, Long groupId, int limit) {
        groupService.ensureMember(groupId, userId);
        List<Message> messages = messageRepository.findGroupChat(groupId, PageRequest.of(0, limit));
        Collections.reverse(messages);
        return messages.stream().map(this::toVO).toList();
    }

    @Transactional
    public void recall(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("消息不存在"));
        if (!message.getFromUserId().equals(userId)) {
            throw new BusinessException("只能撤回自己的消息");
        }
        if (message.getIsRecalled() != null && message.getIsRecalled() == 1) {
            return;
        }
        message.setIsRecalled(1);
        message.setRecalledAt(LocalDateTime.now());
        messageRepository.save(message);
        messagePushService.pushRecall(toVO(message));
    }

    @Transactional
    public void markRead(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("消息不存在"));
        assertCanAccessMessage(userId, message);

        if (messageReadRepository.findByMessageIdAndUserId(messageId, userId).isEmpty()) {
            MessageRead read = new MessageRead();
            read.setMessageId(messageId);
            read.setUserId(userId);
            messageReadRepository.save(read);
        }

        if (message.getChatType() == Constants.CHAT_SINGLE) {
            conversationService.markRead(userId, Constants.TARGET_USER,
                    message.getFromUserId().equals(userId) ? message.getToUserId() : message.getFromUserId(),
                    messageId);
        } else {
            conversationService.markRead(userId, Constants.TARGET_GROUP, message.getGroupId(), messageId);
        }
    }

    private void validateSendRequest(SendMessageRequest request) {
        if (request.getChatType() == Constants.CHAT_SINGLE) {
            if (request.getToUserId() == null) {
                throw new BusinessException("单聊必须指定 toUserId");
            }
        } else if (request.getChatType() == Constants.CHAT_GROUP) {
            if (request.getGroupId() == null) {
                throw new BusinessException("群聊必须指定 groupId");
            }
        } else {
            throw new BusinessException("chatType 无效");
        }
    }

    private String resolveContent(SendMessageRequest request) {
        if (request.getMsgType() == Constants.MSG_TEXT || request.getMsgType() == Constants.MSG_EMOJI) {
            if (request.getContent() == null || request.getContent().isBlank()) {
                throw new BusinessException("消息内容不能为空");
            }
            return request.getContent();
        }
        if (request.getFileId() == null) {
            throw new BusinessException("文件消息必须指定 fileId");
        }
        return request.getContent() != null ? request.getContent() : "";
    }

    private void assertCanAccessMessage(Long userId, Message message) {
        if (message.getChatType() == Constants.CHAT_SINGLE) {
            if (!userId.equals(message.getFromUserId()) && !userId.equals(message.getToUserId())) {
                throw new BusinessException("无权访问该消息");
            }
        } else {
            groupService.ensureMember(message.getGroupId(), userId);
        }
    }

    private MessageVO toVO(Message message) {
        User fromUser = userService.requireUser(message.getFromUserId());
        String fileUrl = null;
        if (message.getFileId() != null) {
            StoredFile file = storedFileRepository.findById(message.getFileId()).orElse(null);
            if (file != null) {
                fileUrl = file.getStoredPath();
            }
        }
        List<Long> mentionedUserIds = null;
        if (message.getMentionUserIds() != null && !message.getMentionUserIds().isBlank()) {
            mentionedUserIds = java.util.Arrays.stream(message.getMentionUserIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .toList();
        }
        return MessageVO.builder()
                .id(message.getId())
                .chatType(message.getChatType())
                .fromUserId(message.getFromUserId())
                .fromNickname(fromUser.getNickname())
                .fromUserAvatar(fromUser.getAvatar())
                .toUserId(message.getToUserId())
                .groupId(message.getGroupId())
                .content(message.getIsRecalled() != null && message.getIsRecalled() == 1 ? null : message.getContent())
                .msgType(message.getMsgType())
                .fileId(message.getFileId())
                .fileUrl(fileUrl)
                .isRecalled(message.getIsRecalled())
                .createdAt(message.getCreatedAt())
                .mentionedUserIds(mentionedUserIds)
                .build();
    }

    public List<Long> searchConversationsByContent(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        List<Message> messages = messageRepository.searchByContent(userId, keyword, PageRequest.of(0, 30));
        var conversationIds = new HashSet<Long>();
        for (Message message : messages) {
            if (message.getChatType() == Constants.CHAT_SINGLE) {
                Long peerId = message.getFromUserId().equals(userId)
                        ? message.getToUserId()
                        : message.getFromUserId();
                conversationRepository.findByUserIdAndTargetTypeAndTargetId(
                        userId, Constants.TARGET_USER, peerId)
                        .ifPresent(conv -> conversationIds.add(conv.getId()));
            } else {
                conversationRepository.findByUserIdAndTargetTypeAndTargetId(
                        userId, Constants.TARGET_GROUP, message.getGroupId())
                        .ifPresent(conv -> conversationIds.add(conv.getId()));
            }
        }
        return new ArrayList<>(conversationIds);
    }
}
