package com.aaa.backend.prescription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 支付处方请求，对应 docs/api.md 9.3。 */
public record PayPrescriptionRequest(
        @NotBlank(message = "支付方式不能为空")
        @Pattern(regexp = "balance|wechat|alipay", message = "支付方式仅支持 balance/wechat/alipay")
        String payMethod
) {
}
