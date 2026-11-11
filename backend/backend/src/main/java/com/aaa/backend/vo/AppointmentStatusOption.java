package com.aaa.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 状态下拉项：数据库原始值 + 中文标签 + 合法流转目标 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentStatusOption {
    private String value;
    private String label;
    private List<String> transitions;
}
