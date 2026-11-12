package com.aaa.backend.doctor.vo;

import lombok.Data;

import java.time.LocalDate;

/** 医生本人排班（含历史与停诊，剩余号源由 SQL 计算） */
@Data
public class DoctorScheduleVO {
    private Long id;
    private LocalDate scheduleDate;
    private String timeSlot;
    private Integer totalQuota;
    private Integer bookedCount;
    private Integer remainQuota;
    private Integer status;
    /** 该排班下的有效预约数（排除已取消） */
    private Long appointmentCount;
}
