package com.xueren.controller;

import com.xueren.common.ApiResponse;
import com.xueren.dto.FileVO;
import com.xueren.security.AuthHolder;
import com.xueren.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ApiResponse<FileVO> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileService.upload(AuthHolder.currentUserId(), file));
    }
}
