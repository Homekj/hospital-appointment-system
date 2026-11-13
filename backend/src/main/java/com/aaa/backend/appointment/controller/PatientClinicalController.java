package com.aaa.backend.appointment.controller;

import com.aaa.backend.appointment.dto.CreateReviewRequest;
import com.aaa.backend.appointment.service.PatientClinicalService;
import com.aaa.backend.appointment.support.CurrentUser;
import com.aaa.backend.appointment.vo.AppointmentReviewVO;
import com.aaa.backend.appointment.vo.ConsultationVO;
import com.aaa.backend.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin
public class PatientClinicalController {

    private final PatientClinicalService patientClinicalService;

    public PatientClinicalController(PatientClinicalService patientClinicalService) {
        this.patientClinicalService = patientClinicalService;
    }

    @GetMapping("/{id}/consultation")
    public ApiResponse<ConsultationVO> consultation(@PathVariable Long id, HttpServletRequest request) {
        Long userId = CurrentUser.requirePatientId(request);
        return ApiResponse.success(patientClinicalService.consultation(userId, id));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<AppointmentReviewVO> review(@PathVariable Long id,
                                                   @Valid @RequestBody CreateReviewRequest body,
                                                   HttpServletRequest request) {
        Long userId = CurrentUser.requirePatientId(request);
        return ApiResponse.success(patientClinicalService.review(userId, id, body));
    }
}
