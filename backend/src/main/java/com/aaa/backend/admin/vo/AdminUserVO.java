package com.aaa.backend.admin.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** User projection that deliberately excludes password, openid, and deleted flag. */
public record AdminUserVO(
        Long id,
        String username,
        String nickname,
        String email,
        String phone,
        String avatar,
        BigDecimal balance,
        Long doctorId,
        Integer status,
        String role,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
