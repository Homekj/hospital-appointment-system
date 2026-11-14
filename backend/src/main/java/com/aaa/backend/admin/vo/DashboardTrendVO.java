package com.aaa.backend.admin.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardTrendVO(LocalDate date, long appointmentCount, BigDecimal revenue) {
}
