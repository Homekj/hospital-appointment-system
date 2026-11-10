package com.aaa.zh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("prescription")
public class Prescription { // 处方表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 关联预约ID
    private Long appointmentId;

    // 关联用户ID
    private Long userId;

    // 开方医生ID
    private Long doctorId;

    // 处方总金额 (decimal 必须用 BigDecimal)
    private BigDecimal totalAmount;

    // 状态 (如：unpaid, paid, dispensed 等)
    private String status;

    // 支付方式
    private String payMethod;

    // 支付时间
    private LocalDateTime payTime;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}