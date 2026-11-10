package com.aaa.backend.config;
//拦截请求，从请求头里查 token，没带或者伪造的直接踢出去（返回 401）

import com.aaa.backend.common.JwtUtil;
import com.aaa.backend.common.Result;
import tools.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    // 用于将 Result 对象转换为 JSON 字符串
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. 放行浏览器的跨域预检请求 (OPTIONS)。如果不放行，前端跨域请求会报错
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 从请求头中获取 Token (前端通常放在 Authorization 字段里)
        String token = request.getHeader("Authorization");

        // 3. 判断 Token 是否为空
        if (token == null || token.trim().isEmpty()) {
            writeErrorResponse(response, 401, "未登录或 Token 缺失，请先登录");
            return false; // 拦截，不放行
        }

        try {
            // 4. 解析 Token (如果 Token 过期、伪造或格式错误，这里会抛出异常)
            Claims claims = JwtUtil.parseToken(token);

            // 5. 提取 Token 中的用户信息和角色
            String role = (String) claims.get("role");
            String userId = String.valueOf(claims.get("userId"));

            // 6. 将解析出的信息存入 request，后续 Controller 可以直接通过 request.getAttribute 获取
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);

            // =====================================================================
            // 7. 核心权限控制：判断当前请求路径
            // 如果是访问后台管理接口（路径包含 /admin），则必须要求角色为 admin
            // =====================================================================
            String requestURI = request.getRequestURI();
            if (requestURI.startsWith("/admin") || requestURI.startsWith("/api/admin")) {
                if (!"admin".equals(role)) {
                    writeErrorResponse(response, 403, "无权限访问，该操作仅限系统管理员");
                    return false;
                }
            }

            // 8. 校验通过，放行请求
            return true;

        } catch (Exception e) {
            // 解析异常（如 Token 过期、签名不匹配等）
            writeErrorResponse(response, 401, "登录已过期或 Token 无效，请重新登录");
            return false;
        }
    }

    /**
     * 辅助方法：向前端输出标准的 JSON 错误信息
     * 防止前端收到 HTML 乱码，导致无法正常弹窗提示
     */
    private void writeErrorResponse(HttpServletResponse response, int status, String message) {
        try {
            // 设置 HTTP 状态码 (401 未登录，403 无权限)
            response.setStatus(status);
            // 设置响应类型为 JSON，字符集为 UTF-8
            response.setContentType("application/json;charset=UTF-8");

            // 调用你项目的统一返回类 Result.error()
            Result errorResult = Result.error(message);

            // 将对象转换为 JSON 字符串并写回给前端
            response.getWriter().write(objectMapper.writeValueAsString(errorResult));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}