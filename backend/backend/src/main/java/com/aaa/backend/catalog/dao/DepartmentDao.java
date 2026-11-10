package com.aaa.backend.catalog.dao;

import com.aaa.backend.entity.Department;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/** 科室通用 CRUD；列表条件用 QueryWrapper 在 Service 里拼，无需自定义 SQL。 */
public interface DepartmentDao extends BaseMapper<Department> {
}
