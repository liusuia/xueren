package com.xueren.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "群名称不能为空")
    private String name;

    @NotEmpty(message = "至少选择一个成员")
    private List<Long> memberIds;
}
