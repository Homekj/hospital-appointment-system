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
@TableName("sys_login_log")
public class SysLoginLog { // 系统登录日志表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 用户名
    private String username;

    // 登录IP地址
    private String ip;

    // 登录状态 (tinyint 映射 Integer，通常 1:成功, 0:失败)
    private Integer status;

    // 提示信息 (如 "登录成功"、"密码错误" 等)
    private String message;

    // 创建时间 (该表没有 update_time)
    private LocalDateTime createTime;
}