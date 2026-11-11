package com.aaa.backend.config;

import com.aaa.backend.file.FileStorageService;
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
    private final String uploadDir;
    private final String[] allowedOrigins;

    public WebMvcConfig(JwtInterceptor jwtInterceptor,
            @Value("${app.upload.dir:./uploads}") String uploadDir,
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://192.168.0.48:5173}")
            String allowedOrigins) {
        this.jwtInterceptor = jwtInterceptor;
        this.uploadDir = uploadDir;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**", "/admin/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With")
                .exposedHeaders("Authorization")
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
    }
}
