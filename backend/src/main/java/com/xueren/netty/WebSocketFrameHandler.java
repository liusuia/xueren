package com.xueren.netty;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xueren.dto.SendMessageRequest;
import com.xueren.security.JwtUtil;
import com.xueren.service.GroupService;
import com.xueren.service.MessageService;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;

@Component
@io.netty.channel.ChannelHandler.Sharable
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final Logger log = LoggerFactory.getLogger(WebSocketFrameHandler.class);
    static final AttributeKey<Long> USER_ID = AttributeKey.valueOf("wsUserId");

    private final ChannelManager channelManager;
    private final MessageService messageService;
    private final GroupService groupService;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    public WebSocketFrameHandler(ChannelManager channelManager,
                                  MessageService messageService,
                                  GroupService groupService,
                                  ObjectMapper objectMapper,
                                  JwtUtil jwtUtil) {
        this.channelManager = channelManager;
        this.messageService = messageService;
        this.groupService = groupService;
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
    }

    private void sendToUser(Long userId, String json) {
        if (userId != null && userId > 0) {
            var ch = channelManager.getChannel(userId);
            if (ch != null && ch.isActive()) {
                ch.writeAndFlush(new TextWebSocketFrame(json));
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete complete) {
            QueryStringDecoder decoder = new QueryStringDecoder(complete.requestUri());
            List<String> tokens = decoder.parameters().get("token");
            if (tokens != null && !tokens.isEmpty()) {
                try {
                    Long userId = jwtUtil.getUserId(tokens.get(0));
                    ctx.channel().attr(USER_ID).set(userId);
                    channelManager.bind(userId, ctx.channel());
                    log.info("WebSocket 握手成功 userId={}", userId);
                } catch (Exception e) {
                    log.warn("WebSocket 握手 token 无效: {}", e.getMessage());
                    ctx.close();
                    return;
                }
            } else {
                log.warn("WebSocket 握手缺少 token");
                ctx.close();
                return;
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        Long userId = ctx.channel().attr(USER_ID).get();
        if (userId == null) {
            ctx.close();
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(frame.text());
            String type = node.path("type").asText();
            if ("PING".equals(type)) {
                ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"PONG\"}"));
                return;
            }
            if ("CHAT".equals(type)) {
                SendMessageRequest request = objectMapper.treeToValue(node.path("data"), SendMessageRequest.class);
                messageService.send(userId, request);
            }
            if ("TYPING".equals(type)) {
                JsonNode data = node.path("data");
                int chatType = data.path("chatType").asInt();
                String json = objectMapper.writeValueAsString(
                    Map.of("type", "TYPING", "data", Map.of(
                        "chatType", chatType,
                        "fromUserId", userId,
                        "toUserId", data.path("toUserId").asLong(0),
                        "groupId", data.path("groupId").asLong(0),
                        "typing", data.path("typing").asBoolean(true)
                    ))
                );
                if (chatType == 1) {
                    sendToUser(data.path("toUserId").asLong(), json);
                } else {
                    groupService.listMemberUserIds(data.path("groupId").asLong())
                        .forEach(uid -> { if (!uid.equals(userId)) sendToUser(uid, json); });
                }
            }
        } catch (Exception ex) {
            log.error("处理消息异常", ex);
            ctx.writeAndFlush(new TextWebSocketFrame(
                    "{\"type\":\"ERROR\",\"data\":{\"message\":\"" + ex.getMessage() + "\"}}"));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelManager.unbind(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket异常", cause);
        channelManager.unbind(ctx.channel());
        ctx.close();
    }
}
