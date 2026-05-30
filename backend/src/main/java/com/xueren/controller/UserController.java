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
