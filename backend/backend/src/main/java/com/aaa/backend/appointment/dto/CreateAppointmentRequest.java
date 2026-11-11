package com.aaa.backend.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 创建预约请求；校验失败由 GlobalExceptionHandler 统一转成 400。 */
public record CreateAppointmentRequest(
        @NotNull(message = "scheduleId 不能为空")
        @Positive(message = "scheduleId 必须为正数")
        Long scheduleId,

        @NotBlank(message = "patientName 不能为空")
        @Size(max = 50, message = "patientName 不能超过 50 个字符")
        String patientName,

        @NotBlank(message = "patientPhone 不能为空")
        @Size(max = 20, message = "patientPhone 不能超过 20 个字符")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String patientPhone,

        @Size(max = 30, message = "idCard 不能超过 30 个字符")
        String idCard,

        @Size(max = 200, message = "remark 不能超过 200 个字符")
        String remark
) {
}
