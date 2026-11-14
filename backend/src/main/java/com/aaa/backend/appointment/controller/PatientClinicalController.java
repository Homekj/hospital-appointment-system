package com.aaa.backend.appointment.controller;

import com.aaa.backend.appointment.dto.CreateReviewRequest;
import com.aaa.backend.appointment.service.PatientClinicalService;
import com.aaa.backend.appointment.vo.AppointmentReviewVO;
import com.aaa.backend.appointment.vo.ConsultationVO;
import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者侧就诊资料接口（docs/api.md 8.7 / 8.8）。
 * 复用 /api/appointments 前缀，但具体路径与 AppointmentController 不重叠，不会冲突。
 */
@RestController
@RequestMapping("/api/appointments")
public class PatientClinicalController {

    private final PatientClinicalService patientClinicalService;
    private final CurrentUserContext currentUserContext;

    public PatientClinicalController(PatientClinicalService patientClinicalService,
                                     CurrentUserContext currentUserContext) {
        this.patientClinicalService = patientClinicalService;
        this.currentUserContext = currentUserContext;
    }

    @GetMapping("/{id}/consultation")
    public ApiResponse<ConsultationVO> consultation(@PathVariable Long id) {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(patientClinicalService.consultation(userId, id));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<AppointmentReviewVO> review(@PathVariable Long id,
                                                   @Valid @RequestBody CreateReviewRequest body) {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(patientClinicalService.review(userId, id, body));
    }
}
