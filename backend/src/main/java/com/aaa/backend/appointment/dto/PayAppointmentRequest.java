package com.aaa.backend.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 挂号费支付请求；wechat / alipay 为模拟支付，balance 走真实余额扣减。 */
public record PayAppointmentRequest(
        @NotBlank(message = "payMethod 不能为空")
        @Pattern(regexp = "wechat|alipay|balance", message = "payMethod 只能是 wechat、alipay 或 balance")
        String payMethod
) {
}
