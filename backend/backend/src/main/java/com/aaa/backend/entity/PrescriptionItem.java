package com.aaa.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("prescription_item")
public class PrescriptionItem { // 处方明细表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 关联处方ID
    private Long prescriptionId;

    // 关联药品ID
    private Long medicineId;

    // 药品名称 (通常明细表会冗余存一下，防止药品改名)
    private String medicineName;

    // 规格
    private String spec;

    // 剂量 (如: 1次2粒)
    private String dosage;

    // 频次 (如: 一日三次)
    private String frequency;

    // 用药天数
    private Integer days;

    // 数量
    private Integer quantity;

    // 单价 (decimal 用 BigDecimal)
    private BigDecimal unitPrice;

    // 总金额 (decimal 用 BigDecimal)
    private BigDecimal amount;
}