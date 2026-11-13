package com.aaa.backend.contract.appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Appointment list/detail projection shared across backend domains. */
public record AppointmentSummary(
        Long id,
        Long userId,
        Long scheduleId,
        Long doctorId,
        String doctorName,
        String doctorTitle,
        Long departmentId,
        String departmentName,
        LocalDate scheduleDate,
        String timeSlot,
        String patientName,
        String patientPhone,
        BigDecimal amount,
        String payMethod,
        LocalDateTime payTime,
        String status,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
