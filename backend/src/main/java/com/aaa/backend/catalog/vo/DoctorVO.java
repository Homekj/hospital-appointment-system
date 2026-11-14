package com.aaa.backend.catalog.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 医生列表/详情项；聚合字段由 DoctorDao 的相关子查询直接映射。 */
@Data
public class DoctorVO {
    private Long id;
    private Long departmentId;
    private String departmentName;
    private String name;
    private String title;
    private Integer gender;
    private String avatar;
    private String intro;
    private BigDecimal rating;
    private Long reviewCount;
    private Long consultCount;
    private Boolean hasAvailableSlot;
    private Integer status;
}
