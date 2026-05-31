package com.xueren.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationVO {

    private Long id;
    private Integer targetType;
    private Long targetId;
    private String targetName;
    private String targetAvatar;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
    private String draft;
    private Boolean targetIsOnline;
}
