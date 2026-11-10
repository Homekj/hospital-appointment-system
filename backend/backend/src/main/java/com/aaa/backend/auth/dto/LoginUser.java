package com.aaa.backend.auth.dto;

import java.math.BigDecimal;

public record LoginUser(
        Long id,
        String username,
        String nickname,
        String email,
        String phone,
        String avatar,
        BigDecimal balance,
        String role,
        Long doctorId,
        Integer status
) {
}
