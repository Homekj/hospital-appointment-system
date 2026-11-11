package com.aaa.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WxLoginRequest(
        @NotBlank(message = "微信登录凭证不能为空")
        @Size(max = 128, message = "微信登录凭证格式不正确")
        String code,
        @Size(max = 50, message = "昵称不能超过 50 个字符")
        String nickname
) {
}
