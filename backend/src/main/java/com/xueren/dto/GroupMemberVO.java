package com.xueren.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupMemberVO {

    private Long userId;
    private String nickname;
    private String remark;
    private String avatar;
    private String username;
    private Integer role;
    private Boolean isMuted;
    private Boolean isNotificationMuted;
    private Boolean isOnline;
}
