package com.aaa.backend.admin.controller;

import com.aaa.backend.admin.dto.DepartmentSaveRequest;
import com.aaa.backend.admin.dto.StatusRequest;
import com.aaa.backend.admin.support.ResultBridge;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.entity.Department;
import com.aaa.backend.service.DepartmentService;
import com.aaa.backend.vo.DepartmentQuery;
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
 * api.md 12.2 科室管理契约。
 *
 * <p>路径、分页参数（current/size）、统一响应体和 HTTP 状态码全部按文档实现；
 * 旧 /admin/department/** 原样保留，按文档 17.7 仅作迁移兼容。
 * 业务逻辑仍走 {@link DepartmentService}，避免在两套接口里出现两套规则。
 */
@RestController
@RequestMapping("/api/admin/departments")
public class AdminDepartmentController {

    private final DepartmentService departmentService;

    public AdminDepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ApiResponse<PageResult<Object>> page(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        DepartmentQuery query = new DepartmentQuery();
        query.setPageNum(current == null || current < 1 ? 1 : current);
        query.setPageSize(size == null || size < 1 ? 10 : Math.min(size, 100));
        query.setKeyword(keyword);
        query.setStatus(status);
        return ResultBridge.page(departmentService.page(query));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<Object> detail(@PathVariable Long id) {
        return ResultBridge.data(departmentService.detail(id));
    }

    @PostMapping
    public ApiResponse<Object> create(@Valid @RequestBody DepartmentSaveRequest request) {
        return ResultBridge.data(departmentService.addDepartment(request.toEntity()));
    }

    @PutMapping("/{id:\\d+}")
    public ApiResponse<Object> update(@PathVariable Long id,
                                      @Valid @RequestBody DepartmentSaveRequest request) {
        Department entity = request.toEntity();
        entity.setId(id);
        return ResultBridge.data(departmentService.updateDepartment(entity));
    }

    @PutMapping("/{id:\\d+}/status")
    public ApiResponse<Object> changeStatus(@PathVariable Long id,
                                            @Valid @RequestBody StatusRequest request) {
        return ResultBridge.data(departmentService.changeStatus(id, request.getStatus()));
    }
}
