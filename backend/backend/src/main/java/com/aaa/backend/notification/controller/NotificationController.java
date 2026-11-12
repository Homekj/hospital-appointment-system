package com.aaa.backend.notification.controller;

import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.notification.service.NotificationService;
import com.aaa.backend.notification.vo.NotificationVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserContext currentUserContext;

    public NotificationController(NotificationService notificationService, CurrentUserContext currentUserContext) {
        this.notificationService = notificationService;
        this.currentUserContext = currentUserContext;
    }

    @GetMapping
    public ApiResponse<PageResult<NotificationVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "current 必须大于或等于 1") long current,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size 必须大于或等于 1")
            @Max(value = 100, message = "size 不能超过 100") int size,
            @RequestParam(required = false) Boolean read) {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(notificationService.page(userId, current, size, read));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(notificationService.unreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        Long userId = currentUserContext.requireUserId();
        notificationService.markRead(userId, id);
        return ApiResponse.success();
    }

    @PutMapping("/read-all")
    public ApiResponse<Long> markAllRead() {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(notificationService.markAllRead(userId));
    }
}
