package com.aaa.zh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("appointment")
public class Appointment {//预约记录
    @TableId(value = "id",type = IdType.AUTO)
    private Long apId;
    @TableField("user_id")
    private Long uid;
    @TableField("schedule_id")
    private Long scId;
    @TableField("patient_name")
    private String patientName;
    @TableField("patient_phone")
    private String patientPhone;
    @TableField("id_card")
    private String idCard;
    private Double amount;
    @TableField("pay_method")
    private String payMethod;
    @TableField("pay_time")
    private LocalDateTime payTime;
    private String status;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
