package com.aaa.backend.admin.controller;

import com.aaa.backend.admin.dto.MedicineSaveRequest;
import com.aaa.backend.admin.dto.StatusRequest;
import com.aaa.backend.admin.log.OperationLog;
import com.aaa.backend.admin.service.AdminDataService;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * api.md 12.7 药品管理：分页查询、详情、新增、修改、启停用。
 * 额外提供删除，但已被处方引用的药品会被拒绝（只能停用）。
 */
@RestController
@RequestMapping("/api/admin/medicines")
public class AdminMedicineController {

    private final AdminDataService adminDataService;

    public AdminMedicineController(AdminDataService adminDataService) {
        this.adminDataService = adminDataService;
    }

    @GetMapping
    public ApiResponse<PageResult<Object>> page(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(adminDataService.medicinePage(current, size, keyword, status));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<Object> detail(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.medicineDetail(id));
    }

    @OperationLog(module = "药品管理", action = "新增药品")
    @PostMapping
    public ApiResponse<Object> create(@Valid @RequestBody MedicineSaveRequest request) {
        return ApiResponse.success(adminDataService.medicineCreate(request));
    }

    @PutMapping("/{id:\\d+}")
    public ApiResponse<Object> update(@PathVariable Long id,
                                      @Valid @RequestBody MedicineSaveRequest request) {
        return ApiResponse.success(adminDataService.medicineUpdate(id, request));
    }

    @PutMapping("/{id:\\d+}/status")
    public ApiResponse<Object> changeStatus(@PathVariable Long id,
                                            @Valid @RequestBody StatusRequest request) {
        adminDataService.medicineStatus(id, request.getStatus());
        return ApiResponse.success(adminDataService.medicineDetail(id));
    }

    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminDataService.medicineDelete(id);
        return ApiResponse.success();
    }
}
