package com.xueren.service;

import com.xueren.common.BusinessException;
import com.xueren.common.Constants;
import com.xueren.dto.MessageVO;
import com.xueren.dto.SendMessageRequest;
import com.xueren.entity.Message;
import com.xueren.entity.MessageHidden;
import com.xueren.entity.MessageRead;
import com.xueren.entity.StoredFile;
import com.xueren.entity.User;
import com.xueren.repository.ConversationRepository;
import com.xueren.repository.MessageHiddenRepository;
import com.xueren.repository.MessageReadRepository;
import com.xueren.repository.MessageRepository;
import com.xueren.repository.StoredFileRepository;
import com.xueren.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final MessageReadRepository messageReadRepository;
    private final StoredFileRepository storedFileRepository;
    private final ConversationRepository conversationRepository;
    private final MessageHiddenRepository messageHiddenRepository;
    private final GroupService groupService;
    private final FriendService friendService;
    private final ConversationService conversationService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final MessagePushService messagePushService;

    public MessageService(MessageRepository messageRepository,
                          MessageReadRepository messageReadRepository,
                          StoredFileRepository storedFileRepository,
                          ConversationRepository conversationRepository,
                          MessageHiddenRepository messageHiddenRepository,
                          UserRepository userRepository,
                          FriendService friendService,
                          GroupService groupService,
                          ConversationService conversationService,
                          UserService userService,
                          MessagePushService messagePushService) {
        this.messageRepository = messageRepository;
        this.messageReadRepository = messageReadRepository;
        this.storedFileRepository = storedFileRepository;
        this.conversationRepository = conversationRepository;
        this.messageHiddenRepository = messageHiddenRepository;
        this.friendService = friendService;
        this.groupService = groupService;
        this.conversationService = conversationService;
        this.userService = userService;
        this.userRepository = userRepository;
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
        message.setReplyToId(request.getReplyToId());
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

    public List<MessageVO> listSingleChat(Long userId, Long peerId, int limit, Long beforeId) {
        LocalDateTime clearedAt = conversationRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, Constants.TARGET_USER, peerId)
                .map(c -> c.getClearedAt())
                .orElse(null);
        List<Message> messages = new ArrayList<>(messageRepository.findSingleChat(userId, peerId, clearedAt, beforeId, PageRequest.of(0, limit)));
        messages = filterHidden(userId, messages);
        Collections.reverse(messages);
        return batchToVO(messages);
    }

    public List<MessageVO> listSingleChat(Long userId, Long peerId, int limit) {
        return listSingleChat(userId, peerId, limit, null);
    }

    public List<MessageVO> listGroupChat(Long userId, Long groupId, int limit, Long beforeId) {
        groupService.ensureMember(groupId, userId);
        LocalDateTime clearedAt = conversationRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, Constants.TARGET_GROUP, groupId)
                .map(c -> c.getClearedAt())
                .orElse(null);
        log.info("listGroupChat userId={} groupId={} clearedAt={} beforeId={}", userId, groupId, clearedAt, beforeId);
        List<Message> messages = new ArrayList<>(messageRepository.findGroupChat(groupId, clearedAt, beforeId, PageRequest.of(0, limit)));
        log.info("listGroupChat after DB query: {} messages", messages.size());
        messages = filterHidden(userId, messages);
        log.info("listGroupChat after filterHidden: {} messages", messages.size());
        Collections.reverse(messages);
        return batchToVO(messages);
    }

    public List<MessageVO> listGroupChat(Long userId, Long groupId, int limit) {
        return listGroupChat(userId, groupId, limit, null);
    }

    /** 批量加载发送者用户，避免 toVO 内 N+1 */
    private List<MessageVO> batchToVO(List<Message> messages) {
        if (messages.isEmpty()) return List.of();
        Set<Long> userIds = messages.stream().map(Message::getFromUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        // 批量加载关联文件
        Set<Long> fileIds = messages.stream().map(Message::getFileId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, StoredFile> fileMap = fileIds.isEmpty() ? Map.of() : storedFileRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(StoredFile::getId, f -> f));
        return messages.stream().map(m -> toVO(m, userMap, fileMap)).toList();
    }

    /** 过滤当前用户隐藏的消息，返回可变列表供后续 Collections.reverse 使用 */
    private List<Message> filterHidden(Long userId, List<Message> messages) {
        if (messages.isEmpty()) return new ArrayList<>(messages);
        try {
            List<Long> msgIds = messages.stream().map(Message::getId).collect(Collectors.toList());
            Set<Long> hiddenIds = messageHiddenRepository.findByUserIdAndMessageIdIn(userId, msgIds)
                    .stream().map(h -> h.getMessageId()).collect(Collectors.toSet());
            if (hiddenIds.isEmpty()) return new ArrayList<>(messages);
            return messages.stream()
                    .filter(m -> !hiddenIds.contains(m.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (Exception e) {
            log.warn("filterHidden failed, returning all messages", e);
            return new ArrayList<>(messages);
        }
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
        // 2分钟内可撤回
        if (message.getCreatedAt() != null &&
                message.getCreatedAt().plusMinutes(2).isBefore(LocalDateTime.now())) {
            throw new BusinessException("超过2分钟的消息无法撤回");
        }
        message.setIsRecalled(1);
        message.setRecalledAt(LocalDateTime.now());
        messageRepository.save(message);
        // 同步更新所有参与者的会话预览（撤回后不显示原内容）
        updateConversationPreviewAfterRecall(message);
        messagePushService.pushRecall(toVO(message));
    }

    private void updateConversationPreviewAfterRecall(Message message) {
        if (message.getChatType() == Constants.CHAT_SINGLE) {
            clearPreviewIfLast(message.getFromUserId(), Constants.TARGET_USER, message.getToUserId(), message.getId());
            clearPreviewIfLast(message.getToUserId(), Constants.TARGET_USER, message.getFromUserId(), message.getId());
        } else {
            groupService.listMemberUserIds(message.getGroupId()).forEach(memberId ->
                clearPreviewIfLast(memberId, Constants.TARGET_GROUP, message.getGroupId(), message.getId())
            );
        }
    }

    private void clearPreviewIfLast(Long userId, Integer targetType, Long targetId, Long messageId) {
        conversationRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .ifPresent(c -> {
                    if (c.getLastMessageId() != null && c.getLastMessageId().equals(messageId)) {
                        c.setLastMessagePreview("[消息已撤回]");
                    }
                });
    }

    @Transactional
    public void markRead(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("消息不存在"));
        assertCanAccessMessage(userId, message);

        if (message.getChatType() == Constants.CHAT_SINGLE) {
            // 单聊：保留逐条已读记录
            if (messageReadRepository.findByMessageIdAndUserId(messageId, userId).isEmpty()) {
                MessageRead read = new MessageRead();
                read.setMessageId(messageId);
                read.setUserId(userId);
                messageReadRepository.save(read);
            }
            conversationService.markRead(userId, Constants.TARGET_USER,
                    message.getFromUserId().equals(userId) ? message.getToUserId() : message.getFromUserId(),
                    messageId);
        } else {
            // 群聊：仅更新 last_read_message_id，不逐条记录（避免 message_read 表爆炸）
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

    private String buildReplyPreview(Message reply) {
        if (reply.getContent() != null && !reply.getContent().isBlank()) {
            String text = reply.getContent();
            return text.length() > 50 ? text.substring(0, 50) + "..." : text;
        }
        return switch (reply.getMsgType() != null ? reply.getMsgType() : 1) {
            case 2 -> "[图片]";
            case 3 -> "[文件]";
            case 4 -> "[表情]";
            default -> "[消息]";
        };
    }

    private String resolveContent(SendMessageRequest request) {
        if (request.getMsgType() == Constants.MSG_TEXT || request.getMsgType() == Constants.MSG_EMOJI || request.getMsgType() == Constants.MSG_CONTACT_CARD) {
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

    /** 单条消息转换（send/recall 场景） */
    private MessageVO toVO(Message message) {
        User fromUser = userRepository.findById(message.getFromUserId()).orElse(null);
        Map<Long, User> userMap = fromUser != null ? Map.of(fromUser.getId(), fromUser) : Map.of();
        StoredFile file = message.getFileId() != null ? storedFileRepository.findById(message.getFileId()).orElse(null) : null;
        Map<Long, StoredFile> fileMap = file != null ? Map.of(file.getId(), file) : Map.of();
        return toVO(message, userMap, fileMap);
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

    private MessageVO toVO(Message message, Map<Long, User> userMap, Map<Long, StoredFile> fileMap) {
        User fromUser = userMap.get(message.getFromUserId());
        String fileUrl = null;
        if (message.getFileId() != null) {
            StoredFile file = fileMap.get(message.getFileId());
            if (file != null) {
                fileUrl = file.getStoredPath();
            }
        }
        // 回复引用信息
        String replyToPreview = null;
        if (message.getReplyToId() != null) {
            Message reply = messageRepository.findById(message.getReplyToId()).orElse(null);
            if (reply != null && reply.getIsRecalled() != null && reply.getIsRecalled() == 1) {
                replyToPreview = "[消息已撤回]";
            } else if (reply != null) {
                replyToPreview = buildReplyPreview(reply);
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
                .fileId(message.getIsRecalled() != null && message.getIsRecalled() == 1 ? null : message.getFileId())
                .fileUrl(message.getIsRecalled() != null && message.getIsRecalled() == 1 ? null : fileUrl)
                .replyToId(message.getReplyToId())
                .replyToPreview(replyToPreview)
                .isRecalled(message.getIsRecalled())
                .editedAt(message.getEditedAt())
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

    @Transactional
    public void clearChatHistory(Long userId, Integer chatType, Long targetId) {
        if (chatType == Constants.CHAT_SINGLE) {
            friendService.ensureFriend(userId, targetId);
        } else {
            groupService.ensureMember(targetId, userId);
        }
        // 软删除：只清空当前用户的视角，设置 clearedAt 为当前时间
        conversationRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, chatType == Constants.CHAT_SINGLE ? Constants.TARGET_USER : Constants.TARGET_GROUP, targetId)
                .ifPresent(conv -> {
                    conv.setClearedAt(LocalDateTime.now());
                    conv.setUnreadCount(0);
                    conversationRepository.save(conv);
                });
    }

    /** 隐藏单条消息（仅对自己不可见，不删除数据库记录） */
    @Transactional
    public MessageVO editMessage(Long userId, Long messageId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("消息不存在"));
        if (!message.getFromUserId().equals(userId)) {
            throw new BusinessException("只能编辑自己的消息");
        }
        if (message.getIsRecalled() != null && message.getIsRecalled() == 1) {
            throw new BusinessException("已撤回的消息无法编辑");
        }
        if (newContent == null || newContent.isBlank()) {
            throw new BusinessException("内容不能为空");
        }
        message.setContent(newContent);
        message.setEditedAt(LocalDateTime.now());
        messageRepository.save(message);
        MessageVO vo = toVO(message);
        // WebSocket 推送编辑后的消息给所有参与者
        messagePushService.pushMessageEdited(vo);
        return vo;
    }

    @Transactional
    public void hideMessage(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("消息不存在"));
        // 验证有权访问该消息
        assertCanAccessMessage(userId, message);
        if (!messageHiddenRepository.existsByUserIdAndMessageId(userId, messageId)) {
            MessageHidden hidden = new MessageHidden();
            hidden.setUserId(userId);
            hidden.setMessageId(messageId);
            messageHiddenRepository.save(hidden);
        }
    }
}
