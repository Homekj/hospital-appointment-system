package com.aaa.backend.catalog.vo;

import lombok.Data;

/** 科室列表项，字段严格对应 docs/api.md 7.1，不直接把实体（含创建时间）暴露给前端。 */
@Data
public class DepartmentVO {
    private Long id;
    private String name;
    private String description;
    private String location;
    private Integer sortOrder;
    private Integer status;
}
