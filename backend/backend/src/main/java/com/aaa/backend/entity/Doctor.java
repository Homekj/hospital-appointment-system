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
@TableName("doctor")
public class Doctor { // 医生表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 科室ID (自动映射: department_id -> departmentId)
    private Long departmentId;

    // 医生姓名
    private String name;

    // 职称 (如：主任医师、副主任医师等)
    private String title;

    // 性别 (tinyint 映射 Integer，通常 1:男, 2:女 或 0:女, 1:男)
    private Integer gender;

    // 头像URL
    private String avatar;

    // 医生简介
    private String intro;

    // 状态 (如：1:在职/出诊, 0:停诊)
    private Integer status;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}