package com.xueren.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private String birthday;
    private String region;
    private LocalDateTime lastOnlineAt;
    private LocalDateTime createdAt;
    private Boolean isOnline;
}
