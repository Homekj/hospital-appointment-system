package com.aaa.backend.service.serviceimpl;

import com.aaa.backend.common.Result;
import com.aaa.backend.dao.DepartmentDao;
import com.aaa.backend.dao.DoctorDao;
import com.aaa.backend.entity.Department;
import com.aaa.backend.entity.Doctor;
import com.aaa.backend.service.DepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceimpl implements DepartmentService {
    @Autowired
    private DepartmentDao departmentDao;
    @Autowired
    private DoctorDao doctorDao;
    @Override
    public Result getByLikeName(String name){
        QueryWrapper<Department> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name",name);
        List<Department> list = departmentDao.selectList(queryWrapper);
        return Result.ok(list);
    }
    @Override
    public Result addDepartment(Department department){
        if (department.getName()==null||department.getName().trim().isEmpty()){
            return Result.error("科室名称不能为空");
        }
        int row = departmentDao.insert(department);
        if (row>0){
            return Result.ok("添加成功");
        }else{
            return Result.error("添加失败");
        }
    }
    @Override
    public  Result updateDepartment(Department department){
        if (department.getId()==null){
            return Result.error("修改失败，ID不能为空");
        }
        if (department.getName()==null||department.getName().trim().isEmpty()){
            return Result.error("科室名称不能为空");
    }
        int row = departmentDao.updateById(department);
        if (row > 0) {
            return Result.ok("修改成功");
        } else {
            return Result.error("修改失败，数据不存在");
        }
    }
    public Result deleteDepartment(Long id){
        QueryWrapper<Doctor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("department_id",id);
        Long doctorCount = doctorDao.selectCount(queryWrapper);
        if (doctorCount>0){
            return Result.error("该科室下有关联医生，无法删除");
        }
        return Result.ok("删除成功");
    }
    @Override
    public Result getByDepartmentName(){
        QueryWrapper<Department> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status",1);
        queryWrapper.select("id", "name");
        queryWrapper.orderByAsc("sort_order");
        List<Department> list = departmentDao.selectList(queryWrapper);
        return Result.ok(list);
    }

}
