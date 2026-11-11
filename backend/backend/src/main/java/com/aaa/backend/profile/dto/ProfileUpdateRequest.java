package com.aaa.backend.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 50, message = "昵称不能超过50个字符")
        String nickname,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱不能超过100个字符")
        String email,

        @Size(max = 20, message = "手机号不能超过20个字符")
        String phone
) {
}