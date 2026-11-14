package com.aaa.backend.doctor.vo;

import lombok.Data;

import java.math.BigDecimal;

/** api.md 13.10 医生开方可选药品：只返回启用且有库存的药品。 */
@Data
public class MedicineOptionVO {
    private Long id;
    private String name;
    private String spec;
    private String unit;
    private BigDecimal price;
    private Integer stock;
    private String usageDesc;
}
