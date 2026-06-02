package com.xueren.controller;

import com.xueren.common.ApiResponse;
import com.xueren.common.Constants;
import com.xueren.entity.RedPacket;
import com.xueren.entity.RedPacketReceive;
import com.xueren.repository.RedPacketReceiveRepository;
import com.xueren.repository.RedPacketRepository;
import com.xueren.security.AuthHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.*;

@RestController
@RequestMapping("/api/red-packet")
public class RedPacketController {

    private final RedPacketRepository packetRepo;
    private final RedPacketReceiveRepository receiveRepo;
    private final SecureRandom rng = new SecureRandom();

    public RedPacketController(RedPacketRepository packetRepo, RedPacketReceiveRepository receiveRepo) {
        this.packetRepo = packetRepo;
        this.receiveRepo = receiveRepo;
    }

    @PostMapping
    @Transactional
    public ApiResponse<RedPacket> create(@RequestBody Map<String, Object> body) {
        Long userId = AuthHolder.currentUserId();
        int amount = ((Number) body.get("amount")).intValue(); // 元
        int count = ((Number) body.get("count")).intValue();
        int chatType = ((Number) body.get("chatType")).intValue();
        long targetId = ((Number) body.get("targetId")).longValue();
        String message = (String) body.getOrDefault("message", "恭喜发财，大吉大利");
        if (amount <= 0 || count <= 0 || count > 100) throw new RuntimeException("参数错误");
        RedPacket p = new RedPacket();
        p.setSenderId(userId); p.setChatType(chatType); p.setTargetId(targetId);
        p.setAmount(amount * 100); p.setCount(count); p.setRemainingCount(count);
        p.setMessage(message);
        packetRepo.save(p);
        return ApiResponse.ok(p);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        RedPacket p = packetRepo.findById(id).orElseThrow(() -> new RuntimeException("红包不存在"));
        List<RedPacketReceive> receives = receiveRepo.findByPacketId(id);
        boolean opened = receiveRepo.existsByPacketIdAndUserId(id, AuthHolder.currentUserId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", p.getId()); result.put("senderId", p.getSenderId());
        result.put("amount", p.getAmount()); result.put("count", p.getCount());
        result.put("remainingCount", p.getRemainingCount());
        result.put("message", p.getMessage()); result.put("opened", opened);
        result.put("receives", receives.stream().map(r -> Map.of("userId", r.getUserId(), "amount", r.getAmount())).toList());
        return ApiResponse.ok(result);
    }

    @PostMapping("/{id}/open")
    @Transactional
    public ApiResponse<Map<String, Object>> open(@PathVariable Long id) {
        Long userId = AuthHolder.currentUserId();
        RedPacket p = packetRepo.findById(id).orElseThrow(() -> new RuntimeException("红包不存在"));
        if (p.getRemainingCount() <= 0) throw new RuntimeException("红包已被领完");
        if (receiveRepo.existsByPacketIdAndUserId(id, userId)) throw new RuntimeException("已领取");
        int myAmount;
        if (p.getRemainingCount() == 1) {
            int received = receiveRepo.findByPacketId(id).stream().mapToInt(RedPacketReceive::getAmount).sum();
            myAmount = p.getAmount() - received;
        } else {
            int avg = p.getAmount() / p.getCount();
            myAmount = Math.max(1, avg / 2 + rng.nextInt(avg));
        }
        RedPacketReceive r = new RedPacketReceive();
        r.setPacketId(id); r.setUserId(userId); r.setAmount(myAmount);
        receiveRepo.save(r);
        p.setRemainingCount(p.getRemainingCount() - 1);
        packetRepo.save(p);
        return ApiResponse.ok(Map.of("amount", myAmount, "remainingCount", p.getRemainingCount()));
    }
}
