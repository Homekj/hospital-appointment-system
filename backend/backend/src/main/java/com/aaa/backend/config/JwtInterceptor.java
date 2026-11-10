package com.aaa.backend.config;

import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.auth.JwtBlacklistService;
import com.aaa.backend.auth.RoleAccessPolicy;
import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.common.JwtUtil;
import com.aaa.backend.service.LoginService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.util.Date;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    private final LoginService loginService;
    private final CurrentUserContext currentUserContext;
    private final ObjectMapper objectMapper;
    private final RoleAccessPolicy roleAccessPolicy;
    private final JwtBlacklistService jwtBlacklistService;

    public JwtInterceptor(
            LoginService loginService,
            CurrentUserContext currentUserContext,
            ObjectMapper objectMapper,
            RoleAccessPolicy roleAccessPolicy,
            JwtBlacklistService jwtBlacklistService
    ) {
        this.loginService = loginService;
        this.currentUserContext = currentUserContext;
        this.objectMapper = objectMapper;
        this.roleAccessPolicy = roleAccessPolicy;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. OPTIONS 预检请求直接放行
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 获取去除 contextPath 后的请求路径
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestURI;
        if (contextPath != null && contextPath.length() > 0 && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        // 2. 明确列出的公开接口放行
        if (isPublicPath(method, path)) {
            return true;
        }

        // 3. 其他 /api/** 接口必须有 Authorization: Bearer <token>
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            writeError(response, 401, "请先登录");
            return false;
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            writeError(response, 401, "请先登录");
            return false;
        }

        try {
            // 4. 解析 Token 的 sub 作为用户 ID
            Claims claims = JwtUtil.parseToken(token);

            String tokenId = claims.getId();
            Date expiration = claims.getExpiration();

            if (tokenId == null || tokenId.isBlank() || expiration == null) {
                writeError(response, 401, "登录令牌无效，请重新登录");
                return false;
            }

            if (jwtBlacklistService.isRevoked(tokenId)) {
                writeError(response, 401, "登录已退出，请重新登录");
                return false;
            }

            Long userId = Long.valueOf(claims.getSubject());
            LoginUser user = loginService.currentUser(userId);

            if(!roleAccessPolicy.isAllowed(path,user.role())){
                writeError(response, 403, "无权访问此资源");
                return false;
            }
            currentUserContext.set(
                    user,
                    tokenId,
                    expiration.toInstant()
            );
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 7. Token 格式错误、签名错误、过期等，统一返回 401
            writeError(response, 401, "登录已过期或无效，请重新登录");
            return false;
        } catch (BusinessException e) {
            writeError(response, e.getErrorCode().code(), e.getMessage());
            return false;
        } catch (Exception e) {
            // 数据库等服务异常不能伪装成认证失败
            log.error("Failed to authenticate API request", e);
            writeError(response, 500, "服务器内部错误");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 8. 请求结束后清理 CurrentUserContext
        currentUserContext.clear();
    }

    /**
     * 判断是否为公开接口
     */
    private boolean isPublicPath(String method, String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        // POST /api/auth/login
        if ("POST".equalsIgnoreCase(method) && "/api/auth/login".equals(path)) {
            return true;
        }

        // POST /api/auth/wx-login
        if ("POST".equalsIgnoreCase(method) && "/api/auth/wx-login".equals(path)) {
            return true;
        }

        // GET /api/departments
        if ("GET".equalsIgnoreCase(method) && "/api/departments".equals(path)) {
            return true;
        }

        // GET /api/doctors 和 GET /api/doctors/{id}
        if ("GET".equalsIgnoreCase(method) && (path.equals("/api/doctors") || path.startsWith("/api/doctors/"))) {
            return true;
        }

        // GET /api/schedules
        if ("GET".equalsIgnoreCase(method) && "/api/schedules".equals(path)) {
            return true;
        }

        // POST /api/triage/analyze
        if ("POST".equalsIgnoreCase(method) && "/api/triage/analyze".equals(path)) {
            return true;
        }

        return false;
    }

    /**
     * 认证错误的响应方法
     */
    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");

        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.failure(code, message)
        );
    }
}
