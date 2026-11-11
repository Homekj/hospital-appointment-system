package com.aaa.backend.service;

import com.aaa.backend.common.Result;
import com.aaa.backend.entity.Department;

public interface DepartmentService {
    public Result getByLikeName(String name);
    public Result addDepartment(Department department);
    public Result updateDepartment(Department department);
    public Result deleteDepartment(Long id);
    public Result getByDepartmentName();
}
