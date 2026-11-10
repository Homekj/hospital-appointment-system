package com.aaa.backend.config;
//拦截请求，从请求头里查 token，没带或者伪造的直接踢出去（返回 401）
import com.aaa.backend.common.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 允许浏览器的跨域预检请求 (OPTIONS 请求)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 从请求头中获取 token (通常前端会把 token 放在 Authorization 字段里)
        String token = request.getHeader("Authorization");

        // 3. 判断 token 是否存在
        if (token == null || token.trim().isEmpty()) {
            response.setStatus(401); // 401 代表未登录
            return false; // 拦截，不放行
        }

        try {
            // 4. 解析 token，如果伪造或过期，这里会抛出异常
            Claims claims = JwtUtil.parseToken(token);

            // 5. 把解析出来的用户信息存入 request，方便后面的 Controller 使用
            request.setAttribute("userId", claims.get("userId"));
            request.setAttribute("role", claims.get("role"));

            // 6. 如果有需要，可以在这里做权限校验（比如判断是不是 admin）
            // String role = (String) claims.get("role");
            // if (!"admin".equals(role)) { ... 拦截 ... }

            return true; // 验证通过，放行
        } catch (Exception e) {
            // token 解析失败（比如过期、被篡改）
            response.setStatus(401);
            return false;
        }
    }
}