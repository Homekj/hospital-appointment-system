package com.aaa.backend.vo;

import lombok.Data;

/** api.md 12.2 科室分页查询条件（旧模块没有科室分页，因此新增）。 */
@Data
public class DepartmentQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private Integer status;
}
