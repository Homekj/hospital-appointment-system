package com.aaa.backend.service;

import com.aaa.backend.common.Result;
import com.aaa.backend.entity.Department;
import com.aaa.backend.vo.DepartmentQuery;

public interface DepartmentService {
    public Result getByLikeName(String name);
    public Result addDepartment(Department department);
    public Result updateDepartment(Department department);
    public Result deleteDepartment(Long id);
    public Result getByDepartmentName();

    /** api.md 12.2 新增的 RESTful 契约能力，旧 /admin/department/** 不调用这些方法。 */
    Result page(DepartmentQuery query);
    Result detail(Long id);
    Result changeStatus(Long id, Integer status);
}
