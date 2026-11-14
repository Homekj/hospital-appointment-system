package com.aaa.backend.contract.appointment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Shared page filters for admin and doctor appointment lists. */
public record AppointmentPageQuery(
        @Min(value = 1, message = "current 必须大于或等于 1") Long current,
        @Min(value = 1, message = "size 必须大于或等于 1")
        @Max(value = 100, message = "size 不能超过 100") Integer size,
        @Pattern(regexp = "unpaid|pending|confirmed|checked_in|in_consultation|pending_rx_pay|completed|cancelled",
                message = "预约状态值不合法") String status,
        @Min(value = 1, message = "doctorId 必须为正数") Long doctorId,
        @Min(value = 1, message = "departmentId 必须为正数") Long departmentId,
        @Size(max = 50, message = "patientName 不能超过 50 个字符") String patientName,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate
) {
    public AppointmentPageQuery {
        current = current == null ? 1L : current;
        size = size == null ? 10 : size;
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate 不能晚于 endDate");
        }
    }
}
