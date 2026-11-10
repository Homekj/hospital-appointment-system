package com.aaa.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role")
public class SysRole { // 系统角色表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 角色编码 (如: ADMIN, DOCTOR, PATIENT)
    private String code;

    // 角色名称
    private String name;

    // 排序权重 (自动映射: sort_order -> sortOrder)
    private Integer sortOrder;

    // 状态 (1:启用, 0:禁用)
    private Integer status;
}