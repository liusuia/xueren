package com.xueren.controller;

import com.xueren.common.ApiResponse;
import com.xueren.dto.MessageVO;
import com.xueren.entity.Favorite;
import com.xueren.entity.Message;
import com.xueren.repository.FavoriteRepository;
import com.xueren.repository.MessageRepository;
import com.xueren.security.AuthHolder;
import com.xueren.service.MessageService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final MessageRepository messageRepository;
    private final MessageService messageService;

    public FavoriteController(FavoriteRepository favoriteRepository, MessageRepository messageRepository,
                               MessageService messageService) {
        this.favoriteRepository = favoriteRepository;
        this.messageRepository = messageRepository;
        this.messageService = messageService;
    }

    @GetMapping
    public ApiResponse<List<MessageVO>> list() {
        Long userId = AuthHolder.currentUserId();
        return ApiResponse.ok(favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(f -> messageRepository.findById(f.getMessageId()).orElse(null))
                .filter(m -> m != null)
                .map(m -> messageService.toVO(m))
                .toList());
    }

    @PostMapping("/{messageId}")
    public ApiResponse<Void> add(@PathVariable Long messageId) {
        Long userId = AuthHolder.currentUserId();
        if (favoriteRepository.existsByUserIdAndMessageId(userId, messageId)) {
            return ApiResponse.fail(400, "已收藏");
        }
        Favorite f = new Favorite();
        f.setUserId(userId);
        f.setMessageId(messageId);
        favoriteRepository.save(f);
        return ApiResponse.ok(null);
    }

    @Transactional
    @DeleteMapping("/{messageId}")
    public ApiResponse<Void> remove(@PathVariable Long messageId) {
        favoriteRepository.deleteByUserIdAndMessageId(AuthHolder.currentUserId(), messageId);
        return ApiResponse.ok(null);
    }
}
