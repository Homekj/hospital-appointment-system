package com.aaa.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("schedule")
public class Schedule { // 排班表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 医生ID
    private Long doctorId;

    // 排班日期 (注意：数据库是 date 类型，这里用 LocalDate)
    private LocalDate scheduleDate;

    // 时间段 (如: "上午", "下午" 或 "08:00-12:00")
    private String timeSlot;

    // 总号源数/总名额
    private BigDecimal registrationFee;
    private Integer totalQuota;

    // 已预约数
    private Integer bookedCount;

    // 状态 (1:正常, 0:停诊)
    private Integer status;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}