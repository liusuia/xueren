package com.xueren.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileVO {

    private Long id;
    private String originalName;
    private String url;
    private Long fileSize;
    private String mimeType;
}
