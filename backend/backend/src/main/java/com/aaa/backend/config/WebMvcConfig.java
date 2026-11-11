package com.aaa.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;

    public WebMvcConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // /api/** 为新版业务接口，/admin/** 为旧后台管理接口（科室/医生/排班等）。
                // RoleAccessPolicy 已为 /api/admin 与 /admin 都配置了 ADMIN 规则，
                // 因此带合法 ADMIN token 可正常访问，非 ADMIN 返回 403，无 token 返回 401。
                .addPathPatterns("/api/**", "/admin/**")
                // 登录接口必须放行；真实路径是 /api/auth/login（旧的 /api/login 已废弃）
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/wx-login",
                        "/api/login",   // 兼容旧调用，放行后返回 404 便于发现路径写错
                        "/error",
                        "/favicon.ico",
                        "/uploads/**"
                );
    }

    /**
     * 全局跨域配置：科室、医生、排班等所有接口统一放行，
     * 避免只有个别 Controller 带 @CrossOrigin 导致前端跨域行为不一致。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }
}