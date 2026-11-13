package com.aaa.backend.admin.controller;

import com.aaa.backend.admin.dto.StatusRequest;
import com.aaa.backend.admin.service.AdminDataService;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * api.md 12.8 用户管理与 12.9 日志查询。
 * 用户接口只返回脱敏字段，不提供密码 / openid 的读取或修改入口。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final AdminDataService adminDataService;

    public AdminUserController(AdminDataService adminDataService) {
        this.adminDataService = adminDataService;
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<Object>> users(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(adminDataService.userPage(current, size, keyword, role, status));
    }

    @GetMapping("/users/{id:\\d+}")
    public ApiResponse<Object> userDetail(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.userDetail(id));
    }

    @PutMapping("/users/{id:\\d+}/status")
    public ApiResponse<Object> userStatus(@PathVariable Long id,
                                          @Valid @RequestBody StatusRequest request) {
        return ApiResponse.success(adminDataService.userStatus(id, request.getStatus()));
    }

    @GetMapping("/logs/login")
    public ApiResponse<PageResult<Object>> loginLogs(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(adminDataService.loginLogs(
                current, size, username, status, startDate, endDate));
    }

    @GetMapping("/logs/operation")
    public ApiResponse<PageResult<Object>> operationLogs(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(adminDataService.operationLogs(
                current, size, username, module, status, startDate, endDate));
    }
}
