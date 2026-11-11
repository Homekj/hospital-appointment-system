package com.aaa.backend.config;

import com.aaa.backend.file.FileStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;
    private final String uploadDir;

    public WebMvcConfig(JwtInterceptor jwtInterceptor,
            @Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.jwtInterceptor = jwtInterceptor;
        this.uploadDir = uploadDir;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**");
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
