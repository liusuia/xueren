package com.xueren.controller;

import com.xueren.common.ApiResponse;
import com.xueren.dto.MessageVO;
import com.xueren.dto.SendMessageRequest;
import com.xueren.security.AuthHolder;
import com.xueren.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ApiResponse<MessageVO> send(@Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(messageService.send(AuthHolder.currentUserId(), request));
    }

    @GetMapping("/single/{peerId}")
    public ApiResponse<List<MessageVO>> singleChat(@PathVariable Long peerId,
                                                   @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(messageService.listSingleChat(AuthHolder.currentUserId(), peerId, limit));
    }

    @GetMapping("/group/{groupId}")
    public ApiResponse<List<MessageVO>> groupChat(@PathVariable Long groupId,
                                                  @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(messageService.listGroupChat(AuthHolder.currentUserId(), groupId, limit));
    }

    @PostMapping("/{messageId}/recall")
    public ApiResponse<Void> recall(@PathVariable Long messageId) {
        messageService.recall(AuthHolder.currentUserId(), messageId);
        return ApiResponse.ok("消息已撤回", null);
    }

    @PostMapping("/{messageId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long messageId) {
        messageService.markRead(AuthHolder.currentUserId(), messageId);
        return ApiResponse.ok("已读", null);
    }

    @GetMapping("/search")
    public ApiResponse<List<Long>> searchConversations(@RequestParam String keyword) {
        return ApiResponse.ok(messageService.searchConversationsByContent(AuthHolder.currentUserId(), keyword));
    }
}
