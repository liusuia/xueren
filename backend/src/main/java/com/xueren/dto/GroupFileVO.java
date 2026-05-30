package com.xueren.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GroupFileVO {

    private Long id;
    private Long groupId;
    private Long uploaderId;
    private String uploaderName;
    private Long fileId;
    private String originalName;
    private String url;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime createdAt;
}
