package com.aaa.backend.doctor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * api.md 13.9 处方明细。
 *
 * <p>刻意不提供 unitPrice / amount 字段：即使客户端硬塞这两个字段，
 * Jackson 默认忽略未知属性，服务端也只用当前 medicine.price 计算金额，
 * 避免客户端伪造药价。
 */
@Data
public class PrescriptionItemRequest {

    @NotNull(message = "medicineId 不能为空")
    private Long medicineId;

    @Size(max = 50, message = "dosage 不能超过 50 个字符")
    private String dosage;

    @Size(max = 50, message = "frequency 不能超过 50 个字符")
    private String frequency;

    @Min(value = 1, message = "days 必须大于 0")
    private Integer days;

    @NotNull(message = "quantity 不能为空")
    @Min(value = 1, message = "quantity 必须大于 0")
    private Integer quantity;
}
