package com.aaa.backend.notification.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内消息项，对应 docs/api.md 10.1；read_flag 已在 SQL 中转成布尔 read。 */
@Data
public class NotificationVO {
    private Long id;
    private String type;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private Boolean read;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
