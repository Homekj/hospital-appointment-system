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
@TableName("department")
public class Department { // 科室表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 科室名称
    private String name;

    // 科室描述
    private String description;

    // 科室位置
    private String location;

    // 排序权重 (自动映射: sort_order -> sortOrder)
    private Integer sortOrder = 0;

    // 状态 (1:启用, 0:禁用)
    private Integer status=1;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}