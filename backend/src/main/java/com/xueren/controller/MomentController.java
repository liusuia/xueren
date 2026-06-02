package com.xueren.controller;

import com.xueren.common.ApiResponse;
import com.xueren.entity.*;
import com.xueren.repository.*;
import com.xueren.security.AuthHolder;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/moments")
public class MomentController {

    private final MomentRepository momentRepo;
    private final MomentLikeRepository likeRepo;
    private final MomentCommentRepository commentRepo;
    private final FriendRepository friendRepo;
    private final UserRepository userRepo;
    private final EntityManager em;

    public MomentController(MomentRepository momentRepo, MomentLikeRepository likeRepo,
                            MomentCommentRepository commentRepo, FriendRepository friendRepo, UserRepository userRepo,
                            EntityManager em) {
        this.momentRepo = momentRepo; this.likeRepo = likeRepo;
        this.commentRepo = commentRepo; this.friendRepo = friendRepo; this.userRepo = userRepo;
        this.em = em;
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Map<String, Object>>> userMoments(@PathVariable Long userId) {
        return ApiResponse.ok(buildItems(momentRepo.findByUserIdOrderByCreatedAtDesc(userId)));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> timeline() {
        Long userId = AuthHolder.currentUserId();
        List<Long> friendIds = friendRepo.findByUserIdAndStatus(userId, 1).stream()
                .map(f -> f.getFriendId()).collect(java.util.stream.Collectors.toList());
        friendIds.add(userId);
        return ApiResponse.ok(buildItems(momentRepo.findByUserIdInOrderByCreatedAtDesc(friendIds)));
    }

    private List<Map<String, Object>> buildItems(List<Moment> moments) {
        Long userId = AuthHolder.currentUserId();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Moment m : moments) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId()); item.put("userId", m.getUserId()); item.put("content", m.getContent());
            item.put("images", m.getImages()); item.put("createdAt", m.getCreatedAt());
            User u = userRepo.findById(m.getUserId()).orElse(null);
            item.put("userName", u != null ? (u.getNickname() != null ? u.getNickname() : u.getUsername()) : "用户");
            item.put("userAvatar", u != null ? u.getAvatar() : null);
            List<MomentLike> likes = likeRepo.findByMomentId(m.getId());
            item.put("likes", likes.stream().map(l -> {
                User lu = userRepo.findById(l.getUserId()).orElse(null);
                return Map.of("userId", l.getUserId(), "name", lu != null ? (lu.getNickname() != null ? lu.getNickname() : lu.getUsername()) : "");
            }).toList());
            item.put("liked", likeRepo.existsByMomentIdAndUserId(m.getId(), userId));
            List<MomentComment> comments = commentRepo.findByMomentIdOrderByCreatedAtAsc(m.getId());
            item.put("comments", comments.stream().map(c -> {
                User cu = userRepo.findById(c.getUserId()).orElse(null);
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("id", c.getId()); cm.put("userId", c.getUserId());
                cm.put("content", c.getContent()); cm.put("createdAt", c.getCreatedAt());
                cm.put("userName", cu != null ? (cu.getNickname() != null ? cu.getNickname() : cu.getUsername()) : "");
                return cm;
            }).toList());
            result.add(item);
        }
        return result;
    }

    @PostMapping
    public ApiResponse<Moment> create(@RequestBody Map<String, String> body) {
        Moment m = new Moment(); m.setUserId(AuthHolder.currentUserId());
        m.setContent(body.getOrDefault("content", ""));
        m.setImages(body.getOrDefault("images", "[]"));
        momentRepo.saveAndFlush(m);
        // 绕过 Hibernate 一级缓存拿到数据库实际值
        return ApiResponse.ok(em.createNativeQuery("SELECT * FROM moment WHERE id=?", Moment.class)
                .setParameter(1, m.getId()).getSingleResult());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Moment m = momentRepo.findById(id).orElse(null);
        if (m == null || !m.getUserId().equals(AuthHolder.currentUserId())) {
            return ApiResponse.fail(403, "无权删除");
        }
        likeRepo.deleteAll(likeRepo.findByMomentId(id));
        commentRepo.deleteAll(commentRepo.findByMomentIdOrderByCreatedAtAsc(id));
        momentRepo.delete(m);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/like")
    @Transactional
    public ApiResponse<Void> like(@PathVariable Long id) {
        Long userId = AuthHolder.currentUserId();
        if (likeRepo.existsByMomentIdAndUserId(id, userId)) {
            likeRepo.deleteByMomentIdAndUserId(id, userId);
        } else {
            MomentLike l = new MomentLike(); l.setMomentId(id); l.setUserId(userId); likeRepo.save(l);
        }
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/comment")
    public ApiResponse<MomentComment> comment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        MomentComment c = new MomentComment(); c.setMomentId(id);
        c.setUserId(AuthHolder.currentUserId()); c.setContent(body.get("content"));
        commentRepo.save(c);
        return ApiResponse.ok(c);
    }
}
