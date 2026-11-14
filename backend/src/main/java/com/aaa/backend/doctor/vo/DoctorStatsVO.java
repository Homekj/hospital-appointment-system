package com.aaa.backend.doctor.vo;

/**
 * api.md 13.2 GET /api/doctor/stats 的返回体。
 * 文档示例只有这四个字段，因此单独建一个精简 VO；
 * 旧 /api/doctor/dashboard 仍返回字段更全的 DoctorDashboardVO，互不干扰。
 */
public record DoctorStatsVO(
        Long todayAppointments,
        Long waitingCount,
        Long inConsultationCount,
        Long completedCount
) {
}
