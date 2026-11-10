package com.aaa.backend.common;

public record ApiResponse<T>(int code, T data, String message) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, data, "success");
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(200, null, "success");
    }

    public static ApiResponse<Void> failure(int code, String message) {
        return new ApiResponse<>(code, null, message);
    }
}
