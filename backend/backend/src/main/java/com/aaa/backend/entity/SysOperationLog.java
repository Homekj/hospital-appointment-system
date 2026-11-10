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
@TableName("sys_operation_log")
public class SysOperationLog { // 系统操作日志表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 操作用户名
    private String username;

    // 操作模块 (如: 用户管理、挂号管理)
    private String module;

    // 操作动作 (如: 新增、修改、删除)
    private String action;

    // 请求方法 (如: GET, POST)
    private String method;

    // 请求URL
    private String url;

    // 请求IP
    private String ip;

    // 消耗时间(毫秒) (自动映射: cost_ms -> costMs)
    private Integer costMs;

    // 操作状态 (1:成功, 0:失败)
    private Integer status;

    // 创建时间 (只有这一个时间字段)
    private LocalDateTime createTime;
}