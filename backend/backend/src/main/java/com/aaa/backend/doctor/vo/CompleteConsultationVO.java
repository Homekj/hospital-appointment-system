package com.aaa.backend.doctor.vo;

import java.math.BigDecimal;

/**
 * api.md 13.9 完成接诊并开方的返回体。
 * 无处方完成时 prescriptionId / prescriptionAmount 均为 null，
 * appointmentStatus 为 completed；有处方时为 pending_rx_pay。
 */
public record CompleteConsultationVO(
        Long appointmentId,
        String appointmentStatus,
        Long prescriptionId,
        BigDecimal prescriptionAmount
) {
}
