package com.aaa.backend.appointment.controller;

import com.aaa.backend.appointment.dto.CreateAppointmentRequest;
import com.aaa.backend.appointment.dto.PayAppointmentRequest;
import com.aaa.backend.appointment.service.AppointmentService;
import com.aaa.backend.appointment.support.CurrentUser;
import com.aaa.backend.appointment.vo.AppointmentDetailVO;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者预约接口。每个方法都从 JWT 取用户 ID，不接受客户端传 userId。
 * 公开查询接口在 CatalogController；本控制器全部要求登录且角色为 USER。
 */
@Validated
@RestController
@RequestMapping("/api/appointments")
@CrossOrigin
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ApiResponse<PageResult<AppointmentDetailVO>> myAppointments(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "current 必须大于或等于 1") long current,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size 必须大于或等于 1")
            @Max(value = 100, message = "size 不能超过 100") int size,
            @RequestParam(required = false) String status) {
        Long userId = CurrentUser.requirePatientId(request);
        return ApiResponse.success(appointmentService.myAppointments(userId, current, size, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<AppointmentDetailVO> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = CurrentUser.requirePatientId(request);
        return ApiResponse.success(appointmentService.detail(userId, id));
    }

    @PostMapping
    public ApiResponse<AppointmentDetailVO> create(
            @Valid @RequestBody CreateAppointmentRequest body, HttpServletRequest request) {
        Long userId = CurrentUser.requirePatientId(request);
        return ApiResponse.success(appointmentService.create(userId, body));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<AppointmentDetailVO> pay(
            @PathVariable Long id,
            @Valid @RequestBody PayAppointmentRequest body,
            HttpServletRequest request) {
        Long userId = CurrentUser.requirePatientId(request);
        return ApiResponse.success(appointmentService.pay(userId, id, body.payMethod()));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<AppointmentDetailVO> cancel(@PathVariable Long id, HttpServletRequest request) {
        Long userId = CurrentUser.requirePatientId(request);
        return ApiResponse.success(appointmentService.cancel(userId, id));
    }

    @PutMapping("/{id}/check-in")
    public ApiResponse<AppointmentDetailVO> checkIn(@PathVariable Long id, HttpServletRequest request) {
        Long userId = CurrentUser.requirePatientId(request);
        return ApiResponse.success(appointmentService.checkIn(userId, id));
    }
}
