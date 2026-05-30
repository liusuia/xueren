package com.xueren.controller;

import com.xueren.common.ApiResponse;
import com.xueren.dto.FriendActionRequest;
import com.xueren.dto.FriendVO;
import com.xueren.dto.RemarkRequest;
import com.xueren.security.AuthHolder;
import com.xueren.service.FriendService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public ApiResponse<List<FriendVO>> listFriends() {
        return ApiResponse.ok(friendService.listFriends(AuthHolder.currentUserId()));
    }

    @GetMapping("/requests")
    public ApiResponse<List<FriendVO>> listRequests() {
        return ApiResponse.ok(friendService.listIncomingRequests(AuthHolder.currentUserId()));
    }

    @PostMapping("/request")
    public ApiResponse<Void> sendRequest(@Valid @RequestBody FriendActionRequest request) {
        friendService.sendRequest(AuthHolder.currentUserId(), request.getFriendId());
        return ApiResponse.ok("好友申请已发送", null);
    }

    @PostMapping("/accept/{requesterId}")
    public ApiResponse<Void> accept(@PathVariable Long requesterId) {
        friendService.acceptRequest(AuthHolder.currentUserId(), requesterId);
        return ApiResponse.ok("已同意好友申请", null);
    }

    @PostMapping("/reject/{requesterId}")
    public ApiResponse<Void> reject(@PathVariable Long requesterId) {
        friendService.rejectRequest(AuthHolder.currentUserId(), requesterId);
        return ApiResponse.ok("已拒绝好友申请", null);
    }

    @PostMapping("/block/{friendId}")
    public ApiResponse<Void> block(@PathVariable Long friendId) {
        friendService.blockFriend(AuthHolder.currentUserId(), friendId);
        return ApiResponse.ok("已拉黑", null);
    }

    @PostMapping("/unblock/{friendId}")
    public ApiResponse<Void> unblock(@PathVariable Long friendId) {
        friendService.unblockFriend(AuthHolder.currentUserId(), friendId);
        return ApiResponse.ok("已解除拉黑", null);
    }

    @GetMapping("/blocked")
    public ApiResponse<List<Long>> listBlocked() {
        return ApiResponse.ok(friendService.listBlockedUserIds(AuthHolder.currentUserId()));
    }

    @GetMapping("/blocked-by")
    public ApiResponse<List<Long>> listBlockedBy() {
        return ApiResponse.ok(friendService.listBlockedByUserIds(AuthHolder.currentUserId()));
    }

    @PutMapping("/{friendId}/remark")
    public ApiResponse<Void> updateRemark(@PathVariable Long friendId,
                                          @Valid @RequestBody RemarkRequest request) {
        friendService.updateRemark(AuthHolder.currentUserId(), friendId, request);
        return ApiResponse.ok("备注已更新", null);
    }

    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> deleteFriend(@PathVariable Long friendId) {
        friendService.deleteFriend(AuthHolder.currentUserId(), friendId);
        return ApiResponse.ok("已删除好友", null);
    }
}
