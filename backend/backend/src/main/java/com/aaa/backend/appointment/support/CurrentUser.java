package com.aaa.backend.appointment.support;

import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.common.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 患者业务专用：从 JWT 解析「当前登录用户」。
 *
 * 背景：项目现有的 JwtInterceptor 只注册在 /admin/** 上，不会拦截 /api/**，
 * 所以患者接口不能依赖 request.getAttribute("userId")，必须自行解析令牌。
 * 客户端传来的 userId 一律不参与数据归属判断（见 docs/api.md 4.3）。
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** 一次解析得到用户 ID 与角色，避免同一个请求里重复解析令牌。 */
    public record Principal(Long userId, String role) {
    }

    public static Principal requireUser(HttpServletRequest request) {
        Claims claims = parse(request);
        return new Principal(readUserId(claims), readRole(claims));
    }

    /** 患者接口专用入口：同时校验角色必须是 USER，否则 403。 */
    public static Long requirePatientId(HttpServletRequest request) {
        Principal principal = requireUser(request);
        if (!"USER".equalsIgnoreCase(principal.role())) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "仅患者账号可访问该接口");
        }
        return principal.userId();
    }

    private static Claims parse(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED);
        }
        String token = header.trim();
        // 同时兼容裸 token 与标准 "Bearer <token>" 两种写法
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        if (token.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED);
        }
        try {
            return JwtUtil.parseToken(token);
        } catch (Exception exception) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
    }

    private static Long readUserId(Claims claims) {
        // 登录时写入的是 Long；JJWT 反序列化后小整数可能变成 Integer，用 Number 统一转换
        Object raw = claims.get("userId");
        if (raw == null) {
            raw = claims.get("sub");
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text.trim());
            } catch (NumberFormatException ignored) {
                // 落到下方统一按无效登录态处理
            }
        }
        throw new BusinessException(BusinessErrorCode.UNAUTHORIZED, "登录信息无效，请重新登录");
    }

    private static String readRole(Claims claims) {
        Object raw = claims.get("role");
        return raw == null ? null : raw.toString();
    }
}
