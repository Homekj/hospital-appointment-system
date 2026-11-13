package com.aaa.backend.wallet.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 余额流水项，对应 docs/api.md 11.2。 */
@Data
public class BalanceLogVO {
    private Long id;
    private Long userId;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private Long orderId;
    private String payMethod;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
