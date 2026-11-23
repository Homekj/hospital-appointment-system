package com.aaa.backend.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ScheduleVO {
    private Long id;
    private Long doctorId;
    private LocalDate scheduleDate;
    private String timeSlot;
    private BigDecimal registrationFee;
    private Integer totalQuota;
    private Integer bookedCount;
    private Integer awaitingVisitCount;
    private Integer status;

    // 联表查询出来的字段
    private String doctorName;
    private String departmentName;

    // 核心要求：剩余号源
    private Integer remainQuota;
}