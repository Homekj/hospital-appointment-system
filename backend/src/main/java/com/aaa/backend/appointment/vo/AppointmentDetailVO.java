package com.aaa.backend.appointment.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约对象，对应 docs/api.md 8.2。
 * 既作为 MyBatis 联表查询的映射目标，也作为接口响应体；
 * 不直接返回实体，避免把数据库内部列名或敏感字段原样带出去。
 */
@Data
public class AppointmentDetailVO {
    private Long id;
    private Long userId;
    private Long scheduleId;
    private Long doctorId;
    private String doctorName;
    private String doctorTitle;
    private String doctorAvatar;
    private Long departmentId;
    private String departmentName;
    private String departmentLocation;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    private String timeSlot;
    private String patientName;
    private String patientPhone;
    private String idCard;
    private BigDecimal amount;
    private String payMethod;
    private String payMethodText;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    private String status;
    private String remark;
    private Long prescriptionId;
    private String prescriptionStatus;
    private BigDecimal prescriptionAmount;
    private Boolean hasConsultation;
    private Boolean evaluated;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
