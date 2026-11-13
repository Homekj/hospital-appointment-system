package com.aaa.backend.vo;

import lombok.Data;

@Data
public class DoctorVo {
    // 医生表基础字段
    private Long id;
    private Long departmentId;
    private String name;
    private String title;
    private Integer gender;
    private String avatar;
    private String intro;
    private Integer status;

    // 联表查询出来的字段
    private String departmentName;
    private String username;
}