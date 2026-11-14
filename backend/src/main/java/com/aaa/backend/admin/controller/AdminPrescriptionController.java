package com.aaa.backend.admin.controller;

import com.aaa.backend.admin.service.AdminOperationsService;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.prescription.vo.PrescriptionDetailVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/prescriptions")
public class AdminPrescriptionController {
    private final AdminOperationsService service;

    public AdminPrescriptionController(AdminOperationsService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<PrescriptionDetailVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) Long current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Min(1) Long userId,
            @RequestParam(required = false) @Min(1) Long doctorId) {
        return ApiResponse.success(service.prescriptions(status, userId, doctorId, current, size));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<PrescriptionDetailVO> detail(@PathVariable @Min(1) Long id) {
        return ApiResponse.success(service.prescriptionDetail(id));
    }

    @PutMapping("/{id:\\d+}/dispense")
    public ApiResponse<PrescriptionDetailVO> dispense(@PathVariable @Min(1) Long id) {
        return ApiResponse.success(service.dispense(id));
    }

    @PutMapping("/{id:\\d+}/cancel")
    public ApiResponse<PrescriptionDetailVO> cancel(@PathVariable @Min(1) Long id) {
        return ApiResponse.success(service.cancelPrescription(id));
    }
}
