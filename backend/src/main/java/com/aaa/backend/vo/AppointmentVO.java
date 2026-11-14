package com.aaa.backend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约列表 / 详情返回体。
 * 由 appointment + schedule + doctor + department 四表联查得出，
 * 带出 doctorName、departmentName、scheduleDate、timeSlot。
 *
 * <p>出于最小必要原则，不返回 id_card。
 */
@Data
public class AppointmentVO {

    private Long id;
    private Long userId;
    private Long scheduleId;

    private Long doctorId;
    private String doctorName;
    private Long departmentId;
    private String departmentName;
    private LocalDate scheduleDate;
    private String timeSlot;

    private String patientName;
    private String patientPhone;

    /** 挂号费：下单时确定，后台只读 */
    private BigDecimal amount;
    private String payMethod;
    private LocalDateTime payTime;

    private String status;
    private String statusLabel;

    /** 当前状态允许流转到的状态值，供前端按需渲染「完成 / 取消 / 标记取药」按钮 */
    private List<String> allowedTransitions;

    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
