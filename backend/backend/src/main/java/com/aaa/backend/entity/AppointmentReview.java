package com.aaa.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("appointment_review")
public class AppointmentReview { // 就诊评价表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("appointment_id")
    private Long appointmentId;

    @TableField("user_id")
    private Long userId;

    @TableField("doctor_id")
    private Long doctorId;

    // tinyint 类型在 Java 中通常映射为 Integer (或者 Byte)
    @TableField("rating")
    private Integer rating;

    @TableField("tags")
    private String tags;

    @TableField("comment")
    private String comment;

    // 注意：这张表只有创建时间，没有更新时间
    @TableField("create_time")
    private LocalDateTime createTime;
}