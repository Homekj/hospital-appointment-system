package com.aaa.backend.admin.controller;

import com.aaa.backend.admin.dto.ScheduleBatchRequest;
import com.aaa.backend.admin.dto.StatusRequest;
import com.aaa.backend.admin.support.ResultBridge;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.contract.appointment.AppointmentCommandService;
import com.aaa.backend.dto.ScheduleSaveDTO;
import com.aaa.backend.service.ScheduleService;
import com.aaa.backend.vo.ScheduleQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * api.md 12.4 排班管理契约。
 *
 * <p>文档查询参数是 startDate / endDate，旧 ScheduleQuery 是
 * scheduleDateStart / scheduleDateEnd；文档批量请求是 startDate / endDate，
 * 旧 DTO 是 scheduleDateStart / scheduleDateEnd。本控制器负责字段适配，
 * 旧 /admin/schedule/** 一行代码都不用改。
 */
@RestController
@RequestMapping("/api/admin/schedules")
public class AdminScheduleController {

    private final ScheduleService scheduleService;
    private final AppointmentCommandService appointments;
    private final CurrentUserContext currentUser;

    public AdminScheduleController(ScheduleService scheduleService, AppointmentCommandService appointments,
                                   CurrentUserContext currentUser) {
        this.scheduleService = scheduleService;
        this.appointments = appointments;
        this.currentUser = currentUser;
    }

    public record StopRequest(@NotBlank @Size(max = 200) String reason) {}
    public record StopResult(int cancelledAppointments) {}

    @PutMapping("/{id}/stop")
    public ApiResponse<StopResult> stop(@PathVariable Long id, @Valid @RequestBody StopRequest request) {
        int cancelled = appointments.stopSchedule(id, request.reason(), currentUser.requireUserId());
        return ApiResponse.success(new StopResult(cancelled));
    }

    @GetMapping
    public ApiResponse<PageResult<Object>> page(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer status) {
        ScheduleQuery query = new ScheduleQuery();
        query.setPageNum(current == null || current < 1 ? 1 : current);
        query.setPageSize(size == null || size < 1 ? 10 : Math.min(size, 100));
        query.setDoctorId(doctorId);
        query.setScheduleDateStart(startDate);
        query.setScheduleDateEnd(endDate);
        query.setStatus(status);
        return ResultBridge.page(scheduleService.pageQuery(query));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<Object> detail(@PathVariable Long id) {
        return ResultBridge.data(scheduleService.detail(id));
    }

    @PostMapping
    public ApiResponse<Object> create(@Valid @RequestBody ScheduleSaveDTO request) {
        return ResultBridge.data(scheduleService.addSchedule(request));
    }

    @PostMapping("/batch")
    public ApiResponse<Object> batch(@Valid @RequestBody ScheduleBatchRequest request) {
        return ResultBridge.data(scheduleService.batchAdd(request.toDto()));
    }

    @PutMapping("/{id:\\d+}")
    public ApiResponse<Object> update(@PathVariable Long id,
                                      @Valid @RequestBody ScheduleSaveDTO request) {
        request.setId(id);
        return ResultBridge.data(scheduleService.update(request));
    }

    @PutMapping("/{id:\\d+}/status")
    public ApiResponse<Object> changeStatus(@PathVariable Long id,
                                            @Valid @RequestBody StatusRequest request) {
        return ResultBridge.data(scheduleService.changeStatus(id, request.getStatus()));
    }
}
