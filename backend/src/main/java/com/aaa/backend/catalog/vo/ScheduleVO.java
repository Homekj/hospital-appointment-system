package com.aaa.backend.catalog.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/** 排班号源项；remainQuota 由 SQL 计算，不放在 Java 里二次加工。 */
@Data
public class ScheduleVO {
    private Long id;
    private Long doctorId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    private String timeSlot;
    private Integer totalQuota;
    private Integer bookedCount;
    private Integer remainQuota;
    private Integer status;
}
