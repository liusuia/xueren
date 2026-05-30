package com.xueren.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendActionRequest {

    @NotNull(message = "好友用户ID不能为空")
    private Long friendId;
}
