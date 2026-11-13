package com.aaa.backend.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * api.md 12.7 药品新增 / 修改请求。
 * 单价与库存不允许为负，状态只允许 0 / 1。
 */
@Data
public class MedicineSaveRequest {

    @NotBlank(message = "药品名称不能为空")
    @Size(max = 100, message = "药品名称最多 100 字符")
    private String name;

    @Size(max = 100, message = "规格最多 100 字符")
    private String spec;

    @Size(max = 20, message = "单位最多 20 字符")
    private String unit;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.00", message = "单价不能为负")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负")
    private Integer stock;

    @Size(max = 200, message = "用法说明最多 200 字符")
    private String usageDesc;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
