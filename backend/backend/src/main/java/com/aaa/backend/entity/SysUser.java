package com.aaa.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class SysUser { // 系统用户表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 登录账号
    private String username;

    // 登录密码 (通常是加密后的密文)
    private String password;

    // 用户昵称
    private String nickname;

    // 邮箱
    private String email;

    // 手机号
    private String phone;

    // 头像URL
    private String avatar;

    // 微信/第三方登录 OpenID
    private String openid;

    // 账户余额 (decimal 必须用 BigDecimal)
    private BigDecimal balance;

    // 关联的医生ID (如果该用户是医生，这里存医生表的主键)
    private Long doctorId;

    // 账号状态 (1:正常, 0:禁用)
    private Integer status;

    // 角色标识 (如 ADMIN, DOCTOR, PATIENT)
    private String role;

    // 逻辑删除标记 (通常 0:未删除, 1:已删除)
    private Integer deleted;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}