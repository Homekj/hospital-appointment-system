package com.aaa.backend.appointment.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价对象。数据库 tags 是英文逗号拼接的字符串，出库时拆成 List 返回给前端，
 * 保持与请求体 tags: string[] 的结构一致。
 */
@Data
public class AppointmentReviewVO {
    private Long id;
    private Long appointmentId;
    private Long userId;
    private Long doctorId;
    private Integer rating;
    private List<String> tags;
    private String comment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
