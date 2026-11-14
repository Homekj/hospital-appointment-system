package com.aaa.backend.admin.controller;

import com.aaa.backend.admin.service.AdminDataService;
import com.aaa.backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * api.md 12.1 数据概览、12.9 菜单。
 * 路径位于 /api/admin 之下，由 JwtInterceptor + RoleAccessPolicy 统一限定 ADMIN。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final AdminDataService adminDataService;

    public AdminDashboardController(AdminDataService adminDataService) {
        this.adminDataService = adminDataService;
    }

    @GetMapping("/dashboard/stats")
    public ApiResponse<Object> stats() {
        return ApiResponse.success(adminDataService.dashboardStats());
    }

    @GetMapping("/menus")
    public ApiResponse<Object> menus() {
        return ApiResponse.success(adminDataService.menus());
    }
}
