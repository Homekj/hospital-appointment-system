package com.aaa.backend.service.serviceimpl;

import com.aaa.backend.common.Result;
import com.aaa.backend.dao.DepartmentDao;
import com.aaa.backend.dao.DoctorDao;
import com.aaa.backend.entity.Department;
import com.aaa.backend.entity.Doctor;
import com.aaa.backend.service.DoctorService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorServiceimpl implements DoctorService {
    @Autowired
    private DoctorDao doctorDao;
    @Autowired
    private DepartmentDao departmentDao;
    @Override
    public Result getByLikeName(String name){
        QueryWrapper<Doctor> queryWrapper = new QueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            queryWrapper.like("name", name);
        }
        List<Doctor> list = doctorDao.selectList(queryWrapper);
        return Result.ok(list);
    }


}

