package com.aaa.backend.admin.log;

import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * 操作日志 AOP 切面。
 *
 * <p>切点覆盖所有 @PostMapping / @PutMapping / @DeleteMapping / @PatchMapping 的写请求，
 * 因此新增接口无需额外改造即可被审计；@OperationLog 注解只用于覆盖自动推导出的模块 / 动作名。
 *
 * <p>记录内容对应 sql/system.sql 的 sys_operation_log：操作人、模块、动作、请求方式、
 * URL、IP、耗时、成功/失败。成功与否同时看异常和响应体 code（
 * 本项目旧 /admin/** 接口失败时是 HTTP 200 + code=500，不能只看 HTTP 状态）。
 *
 * <p>日志写入放在 finally 且用独立事务，写入异常只告警、绝不抛给业务。
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogWriter writer;
    private final CurrentUserContext currentUserContext;
    private final HttpServletRequest request;

    public OperationLogAspect(OperationLogWriter writer,
                              CurrentUserContext currentUserContext,
                              HttpServletRequest request) {
        this.writer = writer;
        this.currentUserContext = currentUserContext;
        this.request = request;
    }

    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PutMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String uri = currentUri();
        if (isIgnored(uri)) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);
        if (annotation != null && annotation.ignore()) {
            return joinPoint.proceed();
        }

        long start = System.currentTimeMillis();
        int status = 1;
        try {
            Object result = joinPoint.proceed();
            status = resolveStatus(result);
            return result;
        } catch (Throwable ex) {
            status = 0;
            throw ex;
        } finally {
            long costMs = System.currentTimeMillis() - start;
            try {
                writer.write(
                        currentUsername(),
                        module(annotation, method),
                        action(annotation, method),
                        truncate(httpMethod(), 10),
                        truncate(uri, 200),
                        clientIp(),
                        (int) Math.min(costMs, Integer.MAX_VALUE),
                        status);
            } catch (Exception writeError) {
                // 审计失败不能影响业务结果
                log.warn("操作日志写入失败: {}", writeError.getMessage());
            }
        }
    }

    /** 登录接口已有 sys_login_log，避免重复记录 */
    private boolean isIgnored(String uri) {
        return "/api/auth/login".equals(uri)
                || "/api/auth/wx-login".equals(uri)
                || "/api/login".equals(uri)
                || "/error".equals(uri);
    }

    /**
     * 业务结果判定：异常 → 失败；响应体 code != 200 → 失败；
     * ResponseEntity 非 2xx → 失败；其它（如 void）视为成功。
     */
    private int resolveStatus(Object result) {
        if (result instanceof ApiResponse<?> response) {
            return response.code() == 200 ? 1 : 0;
        }
        if (result instanceof Result resultBody) {
            return resultBody.code() == 200 ? 1 : 0;
        }
        if (result instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getStatusCode().is2xxSuccessful() ? 1 : 0;
        }
        return 1;
    }

    private String currentUsername() {
        try {
            return truncate(currentUserContext.requireUser().username(), 50);
        } catch (Exception ex) {
            return "anonymous";
        }
    }

    private String clientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlank(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isBlank(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return truncate(isBlank(ip) ? "unknown" : ip, 50);
    }

    private String currentUri() {
        try {
            return request.getRequestURI();
        } catch (Exception ex) {
            return "";
        }
    }

    private String httpMethod() {
        try {
            return request.getMethod();
        } catch (Exception ex) {
            return "";
        }
    }

    private String module(OperationLog annotation, Method method) {
        if (annotation != null && !annotation.module().isBlank()) {
            return truncate(annotation.module().trim(), 50);
        }
        return truncate(moduleOf(method.getDeclaringClass()), 50);
    }

    private String action(OperationLog annotation, Method method) {
        if (annotation != null && !annotation.action().isBlank()) {
            return truncate(annotation.action().trim(), 50);
        }
        return truncate(inferAction(method.getName()), 50);
    }

    /** 按 Controller 类名推导模块；两个同名 AppointmentController 用包名区分新旧端 */
    private static String moduleOf(Class<?> controller) {
        String name = controller.getName();
        String simple = controller.getSimpleName();
        switch (simple) {
            case "AdminDepartmentController":
            case "DepartmentController":
                return "科室管理";
            case "AdminDoctorController":
            case "DoctorController":
                return "医生管理";
            case "AdminScheduleController":
            case "ScheduleController":
                return "排班管理";
            case "AdminMedicineController":
                return "药品管理";
            case "AdminUserController":
                return "用户管理";
            case "AdminDashboardController":
                return "数据概览";
            case "AppointmentController":
                return name.startsWith("com.aaa.backend.appointment.")
                        || name.startsWith("com.aaa.backend.contract.") ? "患者预约" : "预约管理";
            case "PatientClinicalController":
                return "就诊记录";
            case "DoctorWorkbenchController":
                return "医生工作台";
            case "PrescriptionController":
                return "处方管理";
            case "NotificationController":
                return "消息通知";
            case "WalletController":
                return "余额钱包";
            case "ProfileController":
                return "个人资料";
            case "FileUploadController":
                return "文件上传";
            case "TriageController":
                return "智能分诊";
            case "LoginController":
                return "认证登录";
            default:
                return simple.replace("Controller", "");
        }
    }

    /** 按方法名 + HTTP 动词推导动作 */
    private String inferAction(String methodName) {
        String name = methodName.toLowerCase(Locale.ROOT);
        if (name.contains("cancel")) return "取消";
        if (name.contains("checkin") || name.contains("check_in")) return "签到";
        if (name.contains("complete")) return "完成接诊";
        if (name.contains("startconsultation") || name.endsWith("start")) return "开始接诊";
        if (name.contains("batch")) return "批量生成";
        if (name.contains("changestatus") || name.contains("status")) return "启停用";
        if (name.contains("readall")) return "全部标为已读";
        if (name.contains("read")) return "标记已读";
        if (name.contains("review")) return "评价";
        if (name.contains("recharge")) return "充值";
        if (name.contains("pay")) return "支付";
        if (name.contains("avatar")) return "更换头像";
        if (name.contains("upload")) return "上传文件";
        if (name.contains("logout")) return "退出登录";
        if (name.contains("login")) return "登录";
        if (name.contains("delete") || name.contains("remove")) return "删除";
        if (name.contains("insert") || name.contains("create") || name.contains("add")) return "新增";
        if (name.contains("update") || name.contains("edit")) return "修改";

        switch (httpMethod().toUpperCase(Locale.ROOT)) {
            case "POST": return "新增";
            case "PUT":
            case "PATCH": return "修改";
            case "DELETE": return "删除";
            default: return httpMethod();
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank() || "unknown".equalsIgnoreCase(value);
    }
}
