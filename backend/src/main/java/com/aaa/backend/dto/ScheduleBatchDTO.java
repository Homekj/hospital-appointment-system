package com.aaa.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ScheduleBatchDTO {
    // 医生ID列表（前端可能选多个医生一起排班）
    private List<Long> doctorIds;          
    
    // 日期区间
    private LocalDate scheduleDateStart;   
    private LocalDate scheduleDateEnd;     
    
    // 时段列表（前端传 ["08:00", "09:00"]）
    private List<String> timeSlots;        
    
    // 统一的总号源数
    private Integer totalQuota;            
}