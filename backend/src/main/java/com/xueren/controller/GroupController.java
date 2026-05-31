package com.xueren.controller;

import com.xueren.common.ApiResponse;
import com.xueren.dto.CreateGroupRequest;
import com.xueren.dto.GroupFileVO;
import com.xueren.dto.GroupVO;
import com.xueren.security.AuthHolder;
import com.xueren.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ApiResponse<GroupVO> create(@Valid @RequestBody CreateGroupRequest request) {
        return ApiResponse.ok(groupService.createGroup(AuthHolder.currentUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<GroupVO>> list() {
        return ApiResponse.ok(groupService.listMyGroups(AuthHolder.currentUserId()));
    }

    @GetMapping("/{groupId}")
    public ApiResponse<GroupVO> detail(@PathVariable Long groupId) {
        groupService.ensureMember(groupId, AuthHolder.currentUserId());
        return ApiResponse.ok(groupService.getGroupDetail(groupId, AuthHolder.currentUserId()));
    }

    @PostMapping("/{groupId}/members/{userId}")
    public ApiResponse<Void> addMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.addMember(AuthHolder.currentUserId(), groupId, userId);
        return ApiResponse.ok("成员已添加", null);
    }

    @PostMapping("/{groupId}/members")
    public ApiResponse<Void> addMembers(@PathVariable Long groupId, @RequestBody List<Long> userIds) {
        groupService.addMembers(AuthHolder.currentUserId(), groupId, userIds);
        return ApiResponse.ok("成员已添加", null);
    }

    @DeleteMapping("/{groupId}/members/me")
    public ApiResponse<Void> quitGroup(@PathVariable Long groupId) {
        groupService.quitGroup(AuthHolder.currentUserId(), groupId);
        return ApiResponse.ok("已退出群组", null);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.removeMember(AuthHolder.currentUserId(), groupId, userId);
        return ApiResponse.ok("成员已移除", null);
    }

    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> dismissGroup(@PathVariable Long groupId) {
        groupService.dismissGroup(AuthHolder.currentUserId(), groupId);
        return ApiResponse.ok("群组已解散", null);
    }

    @PutMapping("/{groupId}/owner")
    public ApiResponse<Void> transferOwner(@PathVariable Long groupId, @RequestBody Long newOwnerId) {
        groupService.transferOwner(AuthHolder.currentUserId(), groupId, newOwnerId);
        return ApiResponse.ok("群主已转让", null);
    }

    @PutMapping("/{groupId}/remark")
    public ApiResponse<Void> updateMemberNickname(@PathVariable Long groupId, @RequestBody Map<String, String> body) {
        groupService.updateMemberNickname(AuthHolder.currentUserId(), groupId, body.get("remark"));
        return ApiResponse.ok("群备注已更新", null);
    }

    @PutMapping("/{groupId}/my-remark")
    public ApiResponse<Void> updateMyGroupRemark(@PathVariable Long groupId, @RequestBody Map<String, String> body) {
        groupService.updateMyGroupRemark(AuthHolder.currentUserId(), groupId, body.get("remark"));
        return ApiResponse.ok("群聊备注已更新", null);
    }

    @PutMapping("/{groupId}/notice")
    public ApiResponse<Void> updateNotice(@PathVariable Long groupId, @RequestBody Map<String, String> body) {
        groupService.updateNotice(AuthHolder.currentUserId(), groupId, body.get("notice"));
        return ApiResponse.ok("群公告已更新", null);
    }

    @GetMapping("/search")
    public ApiResponse<List<GroupVO>> search(@RequestParam String keyword) {
        return ApiResponse.ok(groupService.searchGroups(keyword));
    }

    @PostMapping("/{groupId}/join")
    public ApiResponse<Void> joinGroup(@PathVariable Long groupId) {
        groupService.joinGroup(AuthHolder.currentUserId(), groupId);
        return ApiResponse.ok("已加入群组", null);
    }

    @PutMapping("/{groupId}/admin/{userId}")
    public ApiResponse<Void> setAdmin(@PathVariable Long groupId, @PathVariable Long userId,
                                       @RequestBody Map<String, Boolean> body) {
        groupService.setAdmin(AuthHolder.currentUserId(), groupId, userId, body.getOrDefault("admin", true));
        return ApiResponse.ok("操作成功", null);
    }

    // 管理员禁言成员
    @PutMapping("/{groupId}/mute/{userId}")
    public ApiResponse<Void> muteMember(@PathVariable Long groupId, @PathVariable Long userId,
                                         @RequestBody Map<String, Boolean> body) {
        groupService.muteMember(AuthHolder.currentUserId(), groupId, userId, body.getOrDefault("mute", true));
        return ApiResponse.ok("操作成功", null);
    }

    // 消息免打扰（自己设自己的）
    @PutMapping("/{groupId}/mute-notification")
    public ApiResponse<Void> muteNotification(@PathVariable Long groupId, @RequestBody Map<String, Boolean> body) {
        groupService.muteNotification(AuthHolder.currentUserId(), groupId, body.getOrDefault("mute", true));
        return ApiResponse.ok("操作成功", null);
    }

    @PostMapping("/{groupId}/files")
    public ApiResponse<GroupFileVO> uploadFile(@PathVariable Long groupId, @RequestBody Map<String, Long> body) {
        return ApiResponse.ok(groupService.uploadGroupFile(
                AuthHolder.currentUserId(), groupId, body.get("fileId")));
    }

    @GetMapping("/{groupId}/files")
    public ApiResponse<List<GroupFileVO>> listFiles(@PathVariable Long groupId) {
        return ApiResponse.ok(groupService.listGroupFiles(AuthHolder.currentUserId(), groupId));
    }

    @PutMapping("/{groupId}/avatar")
    public ApiResponse<GroupVO> updateAvatar(@PathVariable Long groupId, @RequestBody Map<String, String> body) {
        groupService.ensureMember(groupId, AuthHolder.currentUserId());
        return ApiResponse.ok(groupService.updateAvatar(AuthHolder.currentUserId(), groupId, body.get("avatar")));
    }
}
