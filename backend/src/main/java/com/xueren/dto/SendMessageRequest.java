package com.xueren.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SendMessageRequest {

    @NotNull(message = "chatType 不能为空")
    private Integer chatType;

    private Long toUserId;

    private Long groupId;

    private String content;

    @NotNull(message = "msgType 不能为空")
    private Integer msgType;

    private Long fileId;

    private Long replyToId;

    private List<Long> mentionedUserIds;
}
