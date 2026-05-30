package com.xueren.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RemarkRequest {

    @Size(max = 50, message = "备注最多 50 字")
    private String remark;
}
