package com.aaa.backend.doctor.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** api.md 13.5 预约详情里的处方摘要，不返回患者 userId 等无关字段。 */
@Data
public class PrescriptionBriefVO {
    private Long id;
    private Long appointmentId;
    private String status;
    private BigDecimal totalAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
