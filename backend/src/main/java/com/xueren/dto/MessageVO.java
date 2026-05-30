package com.xueren.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MessageVO {

    private Long id;
    private Integer chatType;
    private Long fromUserId;
    private String fromNickname;
    private String fromUserAvatar;
    private Long toUserId;
    private Long groupId;
    private String content;
    private Integer msgType;
    private Long fileId;
    private String fileUrl;
    private Integer isRecalled;
    private LocalDateTime createdAt;
    private List<Long> mentionedUserIds;
}
