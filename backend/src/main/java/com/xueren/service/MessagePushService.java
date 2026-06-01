package com.xueren.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xueren.common.Constants;
import com.xueren.dto.MessageVO;
import com.xueren.netty.ChannelManager;
import com.xueren.netty.WsPacket;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.stereotype.Service;

@Service
public class MessagePushService {

    private final ChannelManager channelManager;
    private final GroupService groupService;
    private final ObjectMapper objectMapper;

    public MessagePushService(ChannelManager channelManager,
                              GroupService groupService,
                              ObjectMapper objectMapper) {
        this.channelManager = channelManager;
        this.groupService = groupService;
        this.objectMapper = objectMapper;
    }

    public void pushNewMessage(MessageVO message) {
        WsPacket packet = new WsPacket();
        packet.setType("NEW_MESSAGE");
        packet.setData(message);
        String json = toJson(packet);
        if (json == null) {
            return;
        }
        if (message.getChatType() == Constants.CHAT_SINGLE) {
            sendToUser(message.getToUserId(), json);
            sendToUser(message.getFromUserId(), json);
        } else if (message.getGroupId() != null) {
            groupService.listMemberUserIds(message.getGroupId()).forEach(userId -> sendToUser(userId, json));
        }
    }

    public void pushMessageEdited(MessageVO message) {
        pushToParticipants(message, "MESSAGE_EDITED");
    }

    public void pushRecall(MessageVO message) {
        pushToParticipants(message, "MESSAGE_RECALLED");
    }

    public void pushReadReceipt(Long messageId, Long senderId) {
        WsPacket packet = new WsPacket();
        packet.setType("READ_RECEIPT");
        packet.setData(java.util.Map.of("messageId", messageId, "readerId", senderId));
        String json = toJson(packet);
        if (json != null) sendToUser(senderId, json);
    }

    private void pushToParticipants(MessageVO message, String type) {
        WsPacket packet = new WsPacket();
        packet.setType(type);
        packet.setData(message);
        String json = toJson(packet);
        if (json == null) {
            return;
        }
        if (message.getChatType() == Constants.CHAT_SINGLE) {
            sendToUser(message.getToUserId(), json);
            sendToUser(message.getFromUserId(), json);
        } else if (message.getGroupId() != null) {
            groupService.listMemberUserIds(message.getGroupId()).forEach(userId -> sendToUser(userId, json));
        }
    }

    private void sendToUser(Long userId, String json) {
        if (userId == null) {
            return;
        }
        Channel channel = channelManager.getChannel(userId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(json));
        }
    }

    private String toJson(WsPacket packet) {
        try {
            return objectMapper.writeValueAsString(packet);
        } catch (Exception ex) {
            return null;
        }
    }
}
