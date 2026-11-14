package com.aaa.backend.admin.support;

import com.aaa.backend.admin.dao.SysOperationLogDao;
import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.entity.SysOperationLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Records admin write operations without request bodies, credentials, or query values. */
@Component
public class AdminOperationLoggingInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AdminOperationLoggingInterceptor.class);
    private static final String START_NANOS = AdminOperationLoggingInterceptor.class.getName() + ".startNanos";

    private final SysOperationLogDao operationLogDao;
    private final CurrentUserContext currentUserContext;

    public AdminOperationLoggingInterceptor(SysOperationLogDao operationLogDao,
                                            CurrentUserContext currentUserContext) {
        this.operationLogDao = operationLogDao;
        this.currentUserContext = currentUserContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_NANOS, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        String method = request.getMethod();
        if (!isAdminPath(request.getRequestURI()) || !isWriteMethod(method)) return;
        try {
            LoginUser user = currentUserContext.requireUser();
            SysOperationLog entry = new SysOperationLog();
            entry.setUsername(limit(user.username(), 50));
            entry.setModule(limit(module(request.getRequestURI()), 50));
            entry.setAction(limit(action(request.getRequestURI(), method), 50));
            entry.setMethod(limit(method, 10));
            entry.setUrl(limit(request.getRequestURI(), 200));
            entry.setIp(limit(request.getRemoteAddr(), 50));
            Object started = request.getAttribute(START_NANOS);
            long elapsed = started instanceof Long start ? (System.nanoTime() - start) / 1_000_000 : 0;
            entry.setCostMs((int) Math.min(Integer.MAX_VALUE, Math.max(0, elapsed)));
            entry.setStatus(exception == null && response.getStatus() < 400 ? 1 : 0);
            operationLogDao.insert(entry);
        } catch (Exception logException) {
            log.warn("Unable to persist admin operation audit record for path={}", request.getRequestURI(), logException);
        }
    }

    private boolean isAdminPath(String path) {
        return path != null && (path.startsWith("/api/admin/") || path.startsWith("/admin/"));
    }

    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
    }

    private String module(String path) {
        String[] segments = path.split("/");
        return segments.length > 3 && "api".equals(segments[1]) && "admin".equals(segments[2])
                ? segments[3] : segments.length > 2 ? segments[2] : "admin";
    }

    private String action(String path, String method) {
        String[] segments = path.split("/");
        String last = segments.length == 0 ? "" : segments[segments.length - 1];
        return last.isBlank() || last.matches("\\d+") ? method.toLowerCase() : last;
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
