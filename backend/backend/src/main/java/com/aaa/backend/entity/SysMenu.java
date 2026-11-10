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
@TableName("sys_menu")
public class SysMenu { // 系统菜单表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 父菜单ID (0表示顶级菜单)
    private Long parentId;

    // 菜单名称
    private String name;

    // 前端路由路径
    private String path;

    // 菜单图标
    private String icon;

    // 排序权重 (自动映射: sort_order -> sortOrder)
    private Integer sortOrder;

    // 权限角色 (如 'ADMIN', 多个角色可能用逗号分隔)
    private String roles;
}