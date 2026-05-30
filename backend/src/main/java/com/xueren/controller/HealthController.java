package com.xueren.controller;

import com.xueren.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public ApiResponse<Map<String, String>> home() {
        return ApiResponse.ok(Map.of(
                "app", "xueren-backend",
                "status", "running",
                "docs", "see backend/README.md"
        ));
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("ok");
    }
}
