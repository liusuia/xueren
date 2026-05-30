package com.xueren.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GroupVO {

    private Long id;
    private String name;
    private String avatar;
    private Long ownerId;
    private LocalDateTime createdAt;
    private List<UserVO> members;
    private List<GroupMemberVO> memberVOs;
    private String remark;
    private String notice;
    private LocalDateTime noticeUpdatedAt;
}
