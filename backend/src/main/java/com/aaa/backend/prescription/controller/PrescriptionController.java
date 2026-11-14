package com.aaa.backend.prescription.controller;

import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.prescription.dto.PayPrescriptionRequest;
import com.aaa.backend.prescription.service.PrescriptionService;
import com.aaa.backend.prescription.vo.PrescriptionDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者处方接口（docs/api.md 9.1 - 9.3）。
 * 横跨 /api/appointments 与 /api/prescriptions 两个前缀，因此直接挂在 /api 下。
 */
@RestController
@RequestMapping("/api")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final CurrentUserContext currentUserContext;

    public PrescriptionController(PrescriptionService prescriptionService, CurrentUserContext currentUserContext) {
        this.prescriptionService = prescriptionService;
        this.currentUserContext = currentUserContext;
    }

    @GetMapping("/appointments/{id}/prescription")
    public ApiResponse<PrescriptionDetailVO> byAppointment(@PathVariable Long id) {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(prescriptionService.byAppointment(userId, id));
    }

    @GetMapping("/prescriptions/{id}")
    public ApiResponse<PrescriptionDetailVO> byId(@PathVariable Long id) {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(prescriptionService.byId(userId, id));
    }

    @PostMapping("/prescriptions/{id}/pay")
    public ApiResponse<PrescriptionDetailVO> pay(@PathVariable Long id,
                                                 @Valid @RequestBody PayPrescriptionRequest body) {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(prescriptionService.pay(userId, id, body.payMethod()));
    }
}
