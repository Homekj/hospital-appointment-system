package com.aaa.backend.vo;

import lombok.Data;

@Data
public class ScheduleQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    
    private Long doctorId;
    private Long departmentId; // 虽然表里没有，但前端传了，我们就在 SQL 里查医生表
    private String scheduleDateStart;
    private String scheduleDateEnd;
    private Integer status;
    private String scheduleDate;
}