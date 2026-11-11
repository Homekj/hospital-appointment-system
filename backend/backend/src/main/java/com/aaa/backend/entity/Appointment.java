package com.aaa.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预约记录（appointment 表）。
 *
 * <p>两个后台约定：
 * <ul>
 *     <li>amount（挂号费）由患者端下单时确定，后台只读，不提供任何修改入口；</li>
 *     <li>status 严格取 sql/system.sql 注释里的七个值，流转规则见
 *         {@link com.aaa.backend.entity.enums.AppointmentStatus}。</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("appointment")
public class Appointment {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("schedule_id")
    private Long scheduleId;

    @TableField("patient_name")
    private String patientName;

    @TableField("patient_phone")
    private String patientPhone;

    /** 身份证号：敏感信息，列表/详情一律不返回，CSV 导出也不包含该列 */
    @TableField("id_card")
    private String idCard;

    /** 挂号费，下单时确定，后台只读 */
    private BigDecimal amount;

    @TableField("pay_method")
    private String payMethod;

    @TableField("pay_time")
    private LocalDateTime payTime;

    /** unpaid / pending / checked_in / in_consultation / pending_rx_pay / completed / cancelled */
    private String status;

    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
