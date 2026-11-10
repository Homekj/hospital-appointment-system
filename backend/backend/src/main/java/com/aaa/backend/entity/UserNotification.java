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
@TableName("user_notification")
public class UserNotification { // 用户通知表

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 接收通知的用户ID
    private Long userId;

    // 通知类型 (如: SYSTEM, APPOINTMENT)
    private String type;

    // 通知标题
    private String title;

    // 通知内容
    private String content;

    // 关联业务类型 (自动映射: biz_type -> bizType)
    private String bizType;

    // 关联业务ID (自动映射: biz_id -> bizId)
    private Long bizId;

    // 阅读状态 (tinyint 映射 Integer，0:未读, 1:已读)
    private Integer readFlag;

    // 创建时间
    private LocalDateTime createTime;
}