package com.aaa.backend.doctor.vo;

import lombok.Data;

import java.time.LocalDate;

/** 医生工作台统计，所有计数都限定在当前医生名下 */
@Data
public class DoctorDashboardVO {
    private LocalDate today;
    /** 累计有效预约（排除已取消） */
    private Long totalAppointments;
    /** 今日就诊的预约（按就诊日期，排除已取消） */
    private Long todayAppointments;
    /** 待接诊：待就诊 + 已到院 */
    private Long waitingCount;
    /** 就诊中 */
    private Long inConsultationCount;
    /** 待缴药费 */
    private Long pendingRxPayCount;
    /** 已完成 */
    private Long completedCount;
    /** 今日排班数 */
    private Long todaySchedules;
    /** 今日剩余号源 */
    private Long todayRemainQuota;
}
