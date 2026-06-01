package com.xueren.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String birthday;
    private String region;
    private String oldPassword;
    private String newPassword;
}
