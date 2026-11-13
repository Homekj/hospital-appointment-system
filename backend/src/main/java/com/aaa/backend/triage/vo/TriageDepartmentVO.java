package com.aaa.backend.triage.vo;

import lombok.Data;

import java.util.List;

/** 分诊推荐的单个科室，附带本次命中得分与命中的症状词。 */
@Data
public class TriageDepartmentVO {
    private Long id;
    private String name;
    private String description;
    private String location;
    private Integer sortOrder;
    private Integer status;
    private Integer score;
    private List<String> symptoms;
}
