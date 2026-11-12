package com.aaa.backend.admin.controller;

import com.aaa.backend.admin.dto.DoctorSaveRequest;
import com.aaa.backend.admin.dto.StatusRequest;
import com.aaa.backend.admin.support.ResultBridge;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.entity.Doctor;
import com.aaa.backend.service.DoctorService;
import com.aaa.backend.vo.DoctorQuery;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * api.md 12.3 医生管理契约。
 *
 * <p>新增医生仍复用旧 Service 的「科室校验 + 自动生成拼音账号 + sys_user 联动」逻辑，
 * 停用医生仍联动禁用登录账号；仅把入口改为 RESTful 路径与文档响应体。
 * 文档没有 DELETE，故本控制器不暴露删除，旧 /admin/doctor/delete 继续可用。
 */
@RestController
@RequestMapping("/api/admin/doctors")
public class AdminDoctorController {

    private final DoctorService doctorService;

    public AdminDoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public ApiResponse<PageResult<Object>> page(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        DoctorQuery query = new DoctorQuery();
        query.setPageNum(current == null || current < 1 ? 1 : current);
        query.setPageSize(size == null || size < 1 ? 10 : Math.min(size, 100));
        query.setDepartmentId(departmentId);
        query.setKeyword(keyword);
        query.setStatus(status);
        return ResultBridge.page(doctorService.page(query));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<Object> detail(@PathVariable Long id) {
        return ResultBridge.data(doctorService.detail(id));
    }

    @PostMapping
    public ApiResponse<Object> create(@Valid @RequestBody DoctorSaveRequest request) {
        return ResultBridge.data(doctorService.add(request.toEntity()));
    }

    @PutMapping("/{id:\\d+}")
    public ApiResponse<Object> update(@PathVariable Long id,
                                      @Valid @RequestBody DoctorSaveRequest request) {
        Doctor entity = request.toEntity();
        entity.setId(id);
        return ResultBridge.data(doctorService.update(entity));
    }

    @PutMapping("/{id:\\d+}/status")
    public ApiResponse<Object> changeStatus(@PathVariable Long id,
                                            @Valid @RequestBody StatusRequest request) {
        return ResultBridge.data(doctorService.changeStatus(id, request.getStatus()));
    }
}
