package com.aaa.backend.entity;

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
@TableName("medicine")
public class Medicine { // 药品表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 药品名称
    private String name;

    // 规格 (如: 0.5g*24粒/盒)
    private String spec;

    // 单位 (如: 盒、瓶)
    private String unit;

    // 单价 (decimal 必须用 BigDecimal)
    private BigDecimal price;

    // 库存数量
    private Integer stock;

    // 用法说明 (自动映射: usage_desc -> usageDesc)
    private String usageDesc;

    // 状态 (1:正常, 0:下架)
    private Integer status;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}