package com.aaa.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ScheduleSaveDTO {
    
    private Long id; // 修改时必传，新增时不传
    
    private Long doctorId;
    
    private LocalDate scheduleDate;
    
    private String timeSlot;
    
    private BigDecimal registrationFee;
    private Integer totalQuota;
    
    private Integer status; // 1正常，0停诊
}