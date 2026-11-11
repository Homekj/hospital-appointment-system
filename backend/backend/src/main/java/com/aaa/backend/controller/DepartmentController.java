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
    // 兼容历史拼写 Lile，建议使用规范的 Like
    @GetMapping({"/findByLikeName"})
    public Result findByLikeName(String name){
        Result result = departmentService.getByLikeName(name);
        return result;
    }
    @PostMapping("/insert")
    public Result insertDepartment(@RequestBody Department department){
        Result result =departmentService.addDepartment(department);
        return result;
    }
    @PutMapping("/update")
    public Result updateDepartment(@RequestBody Department department){
        Result result = departmentService.updateDepartment(department);
        return result;
    }
    @DeleteMapping("/delete")
    public Result deleteDepartment(Long id){
        Result result = departmentService.deleteDepartment(id);
        return result;
    }
    @GetMapping("/findALLDepartment")
    public Result findALLDepartment(){
        Result result = departmentService.getByDepartmentName();
        return result;
    }
}
