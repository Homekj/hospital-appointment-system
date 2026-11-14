package com.aaa.backend.admin.vo;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatsVO(
        long todayAppointments,
        BigDecimal todayRevenue,
        long pendingAppointments,
        long completedAppointments,
        long doctorCount,
        long departmentCount,
        long userCount,
        List<DashboardTrendVO> last7Days
) {
}
