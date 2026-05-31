package com.xueren.event;

import com.xueren.common.Constants;
import com.xueren.dto.MessageVO;
import com.xueren.netty.ChannelManager;
import com.xueren.netty.WsPacket;
import com.xueren.service.MessagePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 群事件监听 — 异步推送，避免循环依赖 */
@Component
public class GroupEventListener {

    private final JdbcTemplate jdbc;
    private final MessagePushService pushService;
    private final ChannelManager channelManager;
    private final ObjectMapper objectMapper;

    public GroupEventListener(JdbcTemplate jdbc, MessagePushService pushService,
                               ChannelManager channelManager, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.pushService = pushService;
        this.channelManager = channelManager;
        this.objectMapper = objectMapper;
    }

    @Async
    @EventListener
    public void onJoinRequest(JoinRequestEvent event) {
        try {
            var ch = channelManager.getChannel(event.ownerId());
            if (ch != null && ch.isActive()) {
                WsPacket packet = new WsPacket();
                packet.setType("JOIN_REQUEST");
                packet.setData(Map.of("groupId", event.groupId()));
                ch.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(packet)));
            }
        } catch (Exception ignored) {}
    }

    @Async
    @EventListener
    public void onGroupEvent(GroupEvent event) {
        try {
            Long msgId = jdbc.queryForObject(
                "SELECT MAX(id) FROM message WHERE group_id=? AND msg_type=?",
                Long.class, event.groupId(), Constants.MSG_SYSTEM);
            if (msgId == null) return;
            MessageVO vo = MessageVO.builder()
                .id(msgId).chatType(Constants.CHAT_GROUP).groupId(event.groupId())
                .content(event.sysMessage()).msgType(Constants.MSG_SYSTEM)
                .isRecalled(0).createdAt(java.time.LocalDateTime.now()).build();
            pushService.pushNewMessage(vo);
        } catch (Exception ignored) {}
    }
}
