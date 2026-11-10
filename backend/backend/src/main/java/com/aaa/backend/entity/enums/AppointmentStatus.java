package com.aaa.backend.entity.enums;

import java.util.Arrays;

public enum AppointmentStatus {
    UNPAID("unpaid"),
    PENDING("pending"),
    CHECKED_IN("checked_in"),
    IN_CONSULTATION("in_consultation"),
    PENDING_RX_PAY("pending_rx_pay"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String value;

    AppointmentStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static AppointmentStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("预约状态不能为空");
        }
        if ("confirmed".equalsIgnoreCase(value)) {
            return PENDING;
        }
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知预约状态: " + value));
    }

    public boolean canPay() {
        return this == UNPAID;
    }

    public boolean canCancelByPatient() {
        return this == UNPAID || this == PENDING;
    }

    public boolean canCancelByAdmin() {
        return this == UNPAID || this == PENDING || this == CHECKED_IN;
    }

    public boolean canCheckIn() {
        return this == PENDING;
    }

    public boolean canStartConsultation() {
        return this == CHECKED_IN;
    }

    public boolean canSaveClinicalRecord() {
        return this == IN_CONSULTATION;
    }

    public boolean canCompleteConsultation() {
        return this == IN_CONSULTATION;
    }

    public boolean canTransitionTo(AppointmentStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case UNPAID -> target == PENDING || target == CANCELLED;
            case PENDING -> target == CHECKED_IN || target == CANCELLED;
            case CHECKED_IN -> target == IN_CONSULTATION || target == CANCELLED;
            case IN_CONSULTATION -> target == PENDING_RX_PAY || target == COMPLETED;
            case PENDING_RX_PAY -> target == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
