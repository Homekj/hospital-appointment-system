package com.aaa.backend.vo;

import lombok.Data;

@Data
public class DoctorQuery {
    // 分页参数
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    // 医生姓名模糊查询
    private String name;
    // 科室名称精确查询
    private String departmentName;
    // 科室 ID 筛选
    private Long departmentId;
    // 兼容前端统一搜索框：name 为空时作为姓名模糊条件
    private String keyword;
    // 状态筛选：0 停用，1 启用
    private Integer status;
}
