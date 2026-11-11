package com.aaa.backend.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 提交评价请求，对应 docs/api.md 8.8。 */
public record CreateReviewRequest(
        @NotNull(message = "评分不能为空")
        @Min(value = 1, message = "评分必须在 1 到 5 之间")
        @Max(value = 5, message = "评分必须在 1 到 5 之间")
        Integer rating,

        @Size(max = 5, message = "最多 5 个标签")
        List<String> tags,

        @Size(max = 500, message = "评价内容不能超过 500 个字符")
        String comment
) {
}
