package com.aaa.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("consultation_record")
public class ConsultationRecord { // 就诊记录表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 关联预约ID
    private Long appointmentId;

    // 医生ID
    private Long doctorId;

    // 主诉
    private String chiefComplaint;

    // 现病史
    private String presentIllness;

    // 诊断结果
    private String diagnosis;

    // 治疗方案
    private String treatmentPlan;

    // 医嘱
    private String doctorAdvice;

    // 创建时间 (依赖数据库默认值)
    private LocalDateTime createTime;

    // 更新时间 (依赖数据库默认值)
    private LocalDateTime updateTime;
}