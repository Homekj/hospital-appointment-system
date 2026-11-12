package com.aaa.backend.controller;

import com.aaa.backend.common.Result;
import com.aaa.backend.entity.Doctor;
import com.aaa.backend.service.DoctorService;
import com.aaa.backend.vo.DoctorQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/doctor")
@CrossOrigin
public class DoctorController {
    @Autowired
    private DoctorService doctorService;

    /** 分页查询：name / keyword / departmentName / departmentId / status / pageNum / pageSize */
    @GetMapping("/page")
    public Result page(DoctorQuery query) {
        return doctorService.page(query);
    }

    /** 医生详情 */
    @GetMapping("/detail")
    public Result detail(@RequestParam Long id) {
        return doctorService.detail(id);
    }

    /** 新增医生 */
    @PostMapping("/insert")
    public Result insert(@RequestBody Doctor doctor) {
        return doctorService.add(doctor);
    }

    /** 修改医生 */
    @PutMapping("/update")
    public Result update(@RequestBody Doctor doctor) {
        return doctorService.update(doctor);
    }

    /** 删除医生（有排班记录时禁止删除） */
    @DeleteMapping("/delete")
    public Result delete(@RequestParam Long id) {
        return doctorService.delete(id);
    }

    /** 启用 / 停用 */
    @PutMapping("/status")
    public Result changeStatus(@RequestParam Long id, @RequestParam Integer status) {
        return doctorService.changeStatus(id, status);
    }

    /** 兼容旧接口：按姓名模糊查询 */
    @GetMapping("/findByLikeName")
    public Result findByLikeName(String name) {
        return doctorService.getByLikeName(name);
    }
    // 下拉框接口
    @GetMapping("/options")
    public Result getOptions() {
        // 直接调用业务层
        return doctorService.listOptions();
    }
}
