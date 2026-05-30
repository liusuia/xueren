package com.xueren.controller;

import com.xueren.common.ApiResponse;
import com.xueren.dto.ConversationVO;
import com.xueren.security.AuthHolder;
import com.xueren.service.ConversationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ApiResponse<List<ConversationVO>> list() {
        return ApiResponse.ok(conversationService.listConversations(AuthHolder.currentUserId()));
    }

    @PostMapping("/read")
    public ApiResponse<Void> markRead(@RequestParam Integer targetType,
                                      @RequestParam Long targetId,
                                      @RequestParam Long lastMessageId) {
        conversationService.markRead(AuthHolder.currentUserId(), targetType, targetId, lastMessageId);
        return ApiResponse.ok("已标记已读", null);
    }

    @DeleteMapping("/{convId}")
    public ApiResponse<Void> delete(@PathVariable Long convId) {
        conversationService.deleteConversation(AuthHolder.currentUserId(), convId);
        return ApiResponse.ok("会话已删除", null);
    }
}
