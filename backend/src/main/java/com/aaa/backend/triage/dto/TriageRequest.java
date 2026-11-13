package com.aaa.backend.triage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 智能分诊请求。symptom 为用户自由输入的症状描述。 */
public record TriageRequest(
        @NotBlank(message = "症状描述不能为空")
        @Size(max = 200, message = "症状描述不能超过 200 个字符")
        String symptom
) {
}
