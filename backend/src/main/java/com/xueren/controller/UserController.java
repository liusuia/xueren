package com.xueren.controller;

import com.xueren.common.ApiResponse;
import com.xueren.dto.FileVO;
import com.xueren.dto.UpdateProfileRequest;
import com.xueren.dto.UserVO;
import com.xueren.security.AuthHolder;
import com.xueren.service.FileService;
import com.xueren.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final FileService fileService;

    public UserController(UserService userService, FileService fileService) {
        this.userService = userService;
        this.fileService = fileService;
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.ok(userService.getById(AuthHolder.currentUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserVO> getUser(@PathVariable Long id) {
        return ApiResponse.ok(userService.getById(id));
    }

    @PostMapping("/email/send-code")
    public ApiResponse<String> sendEmailCode(@RequestBody Map<String, String> body) {
        String code = userService.sendEmailChangeCode(AuthHolder.currentUserId(), body.get("email"));
        return ApiResponse.ok("验证码已发送", code);
    }

    @PutMapping("/email")
    public ApiResponse<UserVO> changeEmail(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(userService.changeEmail(AuthHolder.currentUserId(), body.get("email"), body.get("code")));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount() {
        userService.deleteAccount(AuthHolder.currentUserId());
        return ApiResponse.ok("账号已注销", null);
    }

    @PutMapping("/username")
    public ApiResponse<UserVO> changeUsername(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(userService.changeUsername(AuthHolder.currentUserId(), body.get("username")));
    }

    @GetMapping("/search")
    public ApiResponse<List<UserVO>> search(@RequestParam String keyword) {
        return ApiResponse.ok(userService.search(keyword));
    }

    @PostMapping("/avatar")
    public ApiResponse<UserVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = AuthHolder.currentUserId();
        FileVO fileVO = fileService.upload(userId, file);
        return ApiResponse.ok(userService.updateAvatar(userId, fileVO.getUrl()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserVO> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(AuthHolder.currentUserId(), request));
    }
}
