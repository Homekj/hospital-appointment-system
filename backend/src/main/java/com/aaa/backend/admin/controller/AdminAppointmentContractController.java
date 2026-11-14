package com.aaa.backend.admin.controller;

import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.contract.appointment.AppointmentCommandService;
import com.aaa.backend.contract.appointment.AppointmentPageQuery;
import com.aaa.backend.contract.appointment.AppointmentQueryService;
import com.aaa.backend.contract.appointment.AppointmentSummary;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin appointment API backed by the appointment module's shared contracts. */
@RestController
@RequestMapping("/api/admin/appointments")
public class AdminAppointmentContractController {

    private final AppointmentQueryService queryService;
    private final AppointmentCommandService commandService;
    private final CurrentUserContext currentUserContext;

    public AdminAppointmentContractController(AppointmentQueryService queryService,
                                              AppointmentCommandService commandService,
                                              CurrentUserContext currentUserContext) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.currentUserContext = currentUserContext;
    }

    @GetMapping
    public ApiResponse<PageResult<AppointmentSummary>> page(
            @Valid @ModelAttribute AppointmentPageQuery query) {
        return ApiResponse.success(queryService.pageForAdmin(query));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<AppointmentSummary> detail(@PathVariable Long id) {
        return ApiResponse.success(queryService.getDetailForAdmin(id));
    }

    @PutMapping("/{id:\\d+}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        commandService.cancelByAdmin(id, currentUserContext.requireUserId());
        return ApiResponse.success();
    }
}
