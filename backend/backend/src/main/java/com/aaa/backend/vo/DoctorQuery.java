package com.aaa.backend.vo;

import lombok.Data;

@Data
public class DoctorQuery {
    // 分页参数
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String name;
    private String departmentName;
    private Long departmentId; // 接收下拉框的科室ID
    private String keyword;    // 接收输入框的模糊搜索词
}