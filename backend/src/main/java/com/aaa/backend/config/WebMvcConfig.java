package com.aaa.backend.config;

import com.aaa.backend.file.FileStorageService;
import com.aaa.backend.admin.support.AdminOperationLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;
    private final AdminOperationLoggingInterceptor adminOperationLoggingInterceptor;
    private final String uploadDir;
    private final String[] allowedOrigins;

    public WebMvcConfig(JwtInterceptor jwtInterceptor,
            AdminOperationLoggingInterceptor adminOperationLoggingInterceptor,
            @Value("${app.upload.dir:./uploads}") String uploadDir,
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://192.168.0.48:5173}")
            String allowedOrigins) {
        this.jwtInterceptor = jwtInterceptor;
        this.adminOperationLoggingInterceptor = adminOperationLoggingInterceptor;
        this.uploadDir = uploadDir;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // /api/** 为新版业务接口，/admin/** 为旧后台管理接口（科室/医生/排班/预约）。
                // RoleAccessPolicy 已统一为 /api/admin 与 /admin 仅 ADMIN 可访问。
                .addPathPatterns("/api/**", "/admin/**")
                // 登录接口显式放行（JwtInterceptor.isPublicPath 也会放行，此处作为双保险）
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/wx-login",
                        "/api/login",
                        "/error",
                        "/favicon.ico",
                        "/uploads/**"
                );
        // Registered after JWT so its afterCompletion runs first and can read the authenticated user context.
        registry.addInterceptor(adminOperationLoggingInterceptor)
                .addPathPatterns("/api/admin/**", "/admin/**");
    }

    /**
     * 采用远程的明确来源白名单（可用 CORS_ALLOWED_ORIGINS 覆盖）。
     * 额外暴露 Content-Disposition：预约管理 CSV 导出需要前端读取文件名。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                // Idempotency-Key：api.md 13.9 完成接诊并开方可用该请求头防重复开方
                .allowedHeaders("Authorization", "Content-Type", "Accept", "Origin",
                        "X-Requested-With", "Idempotency-Key")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path avatarRoot = Paths.get(uploadDir).toAbsolutePath().normalize()
                .resolve(FileStorageService.AVATAR_CATEGORY).normalize();
        String location = avatarRoot.toUri().toString();

        if (!location.endsWith("/")) {
            location += "/";
        }

        // Public static access is limited to profile avatars. Sensitive files must use an
        // authenticated download endpoint with an authorization check or a time-limited URL.
        registry.addResourceHandler(FileStorageService.PUBLIC_AVATAR_PATH + "**")
                .addResourceLocations(location);

        Path commonAvatarRoot = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("common");
        String commonAvatarLocation = commonAvatarRoot.toUri().toString();
        if (!commonAvatarLocation.endsWith("/")) {
            commonAvatarLocation += "/";
        }
        registry.addResourceHandler("/uploads/common/**")
                .addResourceLocations(commonAvatarLocation);
    }
}
