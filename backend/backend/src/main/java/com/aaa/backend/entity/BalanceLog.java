package com.aaa.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("balance_log")
public class BalanceLog { // 余额变动日志表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    // 自动映射: user_id -> userId
    private Long userId;

    // 变动类型 (如充值、消费等)
    private String type;

    // 变动金额
    private BigDecimal amount;

    // 变动后余额
    @TableField("balance_after")
    private BigDecimal balanceAfter;

    // 关联订单ID
    @TableField("order_id")
    private Long orderId;

    // 支付方式
    @TableField("pay_method")
    private String payMethod;

    // 备注
    private String remark;

    // 创建时间 (依赖数据库 DEFAULT CURRENT_TIMESTAMP)
    @TableField("create_time")
    private LocalDateTime createTime;
}