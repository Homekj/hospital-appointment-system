package com.aaa.backend.appointment.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 创建预约请求；校验失败由 GlobalExceptionHandler 统一转成 400。 */
public record CreateAppointmentRequest(
        @NotNull(message = "scheduleId 不能为空")
        @Positive(message = "scheduleId 必须为正数")
        Long scheduleId,

        @Size(max = 50, message = "patientName 不能超过 50 个字符")
        String patientName,

        @Size(max = 20, message = "patientPhone 不能超过 20 个字符")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String patientPhone,

        @Size(max = 30, message = "idCard 不能超过 30 个字符")
        String idCard,

        @Size(max = 200, message = "remark 不能超过 200 个字符")
        String remark,

        @Positive Long patientProfileId,

        @DecimalMin("0.01") @Digits(integer = 5, fraction = 2) BigDecimal expectedRegistrationFee
) {
    public CreateAppointmentRequest(Long scheduleId, String patientName, String patientPhone, String idCard, String remark) {
        this(scheduleId, patientName, patientPhone, idCard, remark, null, null);
    }

    @JsonIgnore
    @AssertTrue(message = "请选择常用就诊人，或填写姓名和联系电话")
    public boolean isPatientSpecified() {
        return patientProfileId != null || (patientName != null && !patientName.isBlank() && patientPhone != null);
    }
}
