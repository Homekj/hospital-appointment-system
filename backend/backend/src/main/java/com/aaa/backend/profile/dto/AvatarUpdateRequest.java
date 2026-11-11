package com.aaa.backend.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AvatarUpdateRequest(
        @NotBlank(message = "头像路径不能为空")
        @Size(max = 255, message = "头像路径过长")
        String path
) {
}