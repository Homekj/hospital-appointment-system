package com.aaa.backend.wallet.vo;

import java.math.BigDecimal;

/** 充值结果，对应 docs/api.md 11.1。 */
public record RechargeResultVO(BigDecimal balance, Long logId) {
}
