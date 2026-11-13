package com.aaa.backend.notification.controller;

import com.aaa.backend.appointment.support.CurrentUser;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.notification.service.NotificationService;
import com.aaa.backend.notification.vo.NotificationVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResult<NotificationVO>> page(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "current 必须大于或等于 1") long current,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size 必须大于或等于 1")
            @Max(value = 100, message = "size 不能超过 100") int size,
            @RequestParam(required = false) Boolean read) {
        Long userId = CurrentUser.requirePatientId(request);
        return ApiResponse.success(notificationService.page(userId, current, size, read));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(HttpServletRequest request) {
        Long userId = CurrentUser.requirePatientId(request);
        return ApiResponse.success(notificationService.unreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = CurrentUser.requirePatientId(request);
        notificationService.markRead(userId, id);
        return ApiResponse.success();
    }

    @PutMapping("/read-all")
    public ApiResponse<Long> markAllRead(HttpServletRequest request) {
        Long userId = CurrentUser.requirePatientId(request);
        return ApiResponse.success(notificationService.markAllRead(userId));
    }
}
