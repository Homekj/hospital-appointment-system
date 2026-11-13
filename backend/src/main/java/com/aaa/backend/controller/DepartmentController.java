package com.aaa.backend.controller;

import com.aaa.backend.common.Result;
import com.aaa.backend.entity.Department;
import com.aaa.backend.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/department")
public class DepartmentController {
    @Autowired
    private DepartmentService departmentService;

    /** 兼容历史拼写 Lile，新代码请统一使用 findByLikeName */
    @GetMapping({"/findByLikeName", "/findByLileName"})
    public Result findByLikeName(String name) {
        return departmentService.getByLikeName(name);
    }

    @PostMapping("/insert")
    public Result insertDepartment(@RequestBody Department department) {
        return departmentService.addDepartment(department);
    }

    @PutMapping("/update")
    public Result updateDepartment(@RequestBody Department department) {
        return departmentService.updateDepartment(department);
    }

    @DeleteMapping("/delete")
    public Result deleteDepartment(@RequestParam Long id) {
        return departmentService.deleteDepartment(id);
    }

    /** 启用科室下拉框，返回 [{id, name}] */
    @GetMapping("/findALLDepartment")
    public Result findALLDepartment() {
        return departmentService.getByDepartmentName();
    }
}
