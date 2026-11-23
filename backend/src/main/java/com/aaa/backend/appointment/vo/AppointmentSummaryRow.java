package com.aaa.backend.appointment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * admin / doctor 域预约列表的数据库投影。
 *
 * 为什么不直接映射到 contract 包里的 AppointmentSummary：
 * 那是 Java record（只有全参构造、没有 setter），MyBatis 默认按「无参构造 + setter」
 * 或「按参数位置」自动映射构造器；该 record 有 19 个参数且大量同类型字段（多个 Long / String），
 * 按位置映射极易错位且不会报错。因此先用本类接住结果集，再由 Service 显式转成 record。
 */
@Data
public class AppointmentSummaryRow {
    private Long id;
    private Long userId;
    private Long scheduleId;
    private Long doctorId;
    private String doctorName;
    private String doctorTitle;
    private Long departmentId;
    private String departmentName;
    private LocalDate scheduleDate;
    private String timeSlot;
    private String patientName;
    private String patientPhone;
    private BigDecimal amount;
    private String payMethod;
    private LocalDateTime payTime;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime queueDeferredAt;
}
