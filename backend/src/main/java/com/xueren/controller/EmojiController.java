package com.xueren.controller;

import com.xueren.common.ApiResponse;
import com.xueren.security.AuthHolder;
import com.xueren.entity.Emoji;
import com.xueren.repository.EmojiRepository;
import com.xueren.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@RestController
@RequestMapping("/api/emoji")
public class EmojiController {

    private final EmojiRepository emojiRepository;
    private final FileService fileService;

    public EmojiController(EmojiRepository emojiRepository, FileService fileService) {
        this.emojiRepository = emojiRepository;
        this.fileService = fileService;
    }

    @GetMapping
    public ApiResponse<List<Emoji>> list() {
        return ApiResponse.ok(emojiRepository.findByUserIdOrderByCreatedAtDesc(AuthHolder.currentUserId()));
    }

    @PostMapping
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "name", defaultValue = "") String name) {
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file.getBytes()));
            Long userId = AuthHolder.currentUserId();
            if (emojiRepository.existsByUserIdAndFileHash(userId, hash)) {
                return ApiResponse.fail(400, "该表情已存在");
            }
            var fileVO = fileService.upload(userId, file);
            Emoji emoji = new Emoji();
            emoji.setUserId(userId);
            emoji.setName(name.isBlank() ? file.getOriginalFilename() : name);
            emoji.setUrl(fileVO.getUrl());
            emoji.setFileId(fileVO.getId());
            emoji.setFileHash(hash);
            return ApiResponse.ok(emojiRepository.save(emoji));
        } catch (Exception e) {
            return ApiResponse.fail(500, "上传失败");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        if (!emojiRepository.existsByUserIdAndId(AuthHolder.currentUserId(), id)) {
            return ApiResponse.fail(404, "表情不存在");
        }
        emojiRepository.deleteById(id);
        return ApiResponse.ok(null);
    }
}
