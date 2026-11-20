package com.aaa.backend.doctor.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 医生开方可选药品；stock 是扣除未取处方占用后的可开数量。 */
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
