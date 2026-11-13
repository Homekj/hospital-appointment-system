package com.aaa.backend.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * api.md 12.2 / 12.3 / 12.4 中三个「启用或停用」接口的统一请求体：
 * {"status": 1}。通用启用状态只有 1 / 0 两个值（api.md 5.3）。
 */
@Data
public class StatusRequest {

    @NotNull(message = "status 不能为空")
    @Min(value = 0, message = "status 只能是 0(停用) 或 1(启用)")
    @Max(value = 1, message = "status 只能是 0(停用) 或 1(启用)")
    private Integer status;
}
