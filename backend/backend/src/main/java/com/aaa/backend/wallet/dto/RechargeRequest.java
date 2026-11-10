package com.aaa.backend.wallet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/** 余额充值请求；充值只支持模拟支付，不允许用 balance 自己给自己充值。 */
public record RechargeRequest(
        @NotNull(message = "amount 不能为空")
        @DecimalMin(value = "0.01", message = "充值金额不能小于 0.01")
        @DecimalMax(value = "10000.00", message = "充值金额不能超过 10000.00")
        BigDecimal amount,

        @NotBlank(message = "payMethod 不能为空")
        @Pattern(regexp = "wechat|alipay", message = "payMethod 只能是 wechat 或 alipay")
        String payMethod
) {
}
