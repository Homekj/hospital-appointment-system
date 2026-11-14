package com.aaa.backend.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AdminLogPageQuery(
        @Min(value = 1, message = "current 必须大于或等于 1") Long current,
        @Min(value = 1, message = "size 必须大于或等于 1") @Max(value = 100, message = "size 不能超过 100") Integer size,
        @Size(max = 50, message = "username 不能超过 50 个字符") String username,
        @Size(max = 50, message = "module 不能超过 50 个字符") String module,
        @Min(value = 0, message = "status 只能是 0 或 1") @Max(value = 1, message = "status 只能是 0 或 1") Integer status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
) {
    public AdminLogPageQuery {
        current = current == null ? 1L : current;
        size = size == null ? 10 : size;
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate 不能晚于 endDate");
        }
    }
}
