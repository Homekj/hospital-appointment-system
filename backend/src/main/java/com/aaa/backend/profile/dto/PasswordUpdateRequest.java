package com.aaa.backend.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(
        @NotBlank(message = "网页登录密码不能为空")
        @Size(min = 8, max = 72, message = "网页登录密码长度须为 8 到 72 个字符")
        String password
) {
}
