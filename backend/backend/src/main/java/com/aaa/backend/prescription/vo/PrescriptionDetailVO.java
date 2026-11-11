package com.aaa.backend.prescription.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 处方对象，对应 docs/api.md 9.2。 */
@Data
public class PrescriptionDetailVO {
    private Long id;
    private Long appointmentId;
    private Long userId;
    private Long doctorId;
    private String doctorName;
    private String departmentName;
    private BigDecimal totalAmount;
    private String status;
    private String payMethod;
    private String payMethodText;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private List<PrescriptionItemVO> items;
}
