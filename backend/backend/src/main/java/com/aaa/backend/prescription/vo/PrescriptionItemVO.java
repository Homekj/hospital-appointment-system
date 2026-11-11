package com.aaa.backend.prescription.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 处方明细，字段全部来自开方时的快照。 */
@Data
public class PrescriptionItemVO {
    private Long id;
    private Long prescriptionId;
    private Long medicineId;
    private String medicineName;
    private String spec;
    private String dosage;
    private String frequency;
    private Integer days;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
