package com.aaa.backend.dto;

import lombok.Data;

/**
 * 改状态请求体。
 * 只允许传目标状态；amount、patientPhone 等字段即使传了也不会被读取，
 * 保证挂号费与患者信息不会被后台动作篡改。
 */
@Data
public class AppointmentStatusDTO {
    private String status;
}
