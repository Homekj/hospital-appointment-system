package com.aaa.backend.doctor.vo;

import lombok.Data;

/** 当前登录医生的信息，只取自 JWT 解析出的 doctorId，不接受任何入参 id */
@Data
public class DoctorProfileVO {
    private Long id;
    private String name;
    private String title;
    private Integer gender;
    private String avatar;
    private String intro;
    private Integer status;
    private Long departmentId;
    private String departmentName;
}
