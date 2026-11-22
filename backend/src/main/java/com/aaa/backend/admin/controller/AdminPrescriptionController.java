package com.aaa.backend.admin.controller;

import com.aaa.backend.admin.service.AdminOperationsService;
import com.aaa.backend.admin.service.PrescriptionRefundService;
import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.prescription.vo.PrescriptionDetailVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/prescriptions")
public class AdminPrescriptionController {
    private final AdminOperationsService service;

    private final PrescriptionRefundService refunds;
    private final CurrentUserContext currentUser;

    public AdminPrescriptionController(AdminOperationsService service, PrescriptionRefundService refunds,
                                       CurrentUserContext currentUser) {
        this.service = service;
        this.refunds = refunds;
        this.currentUser = currentUser;
    }

    public record RefundRequest(@NotBlank @Size(max = 200) String reason) {}

    @PutMapping("/{id}/refund")
    public ApiResponse<PrescriptionDetailVO> refund(@PathVariable @Min(1) Long id,
                                                     @Valid @RequestBody RefundRequest body) {
        return ApiResponse.success(refunds.refund(id, body.reason(), currentUser.requireUserId()));
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
