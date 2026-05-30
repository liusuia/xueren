package com.xueren.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FriendVO {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String remark;
    private Integer status;
    private Long requesterId;
    private LocalDateTime createdAt;
    private Boolean isOnline;
}
