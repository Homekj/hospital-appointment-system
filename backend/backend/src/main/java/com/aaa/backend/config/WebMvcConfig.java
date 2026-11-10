package com.aaa.backend.config;
//注册拦截器
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册我们刚才写的拦截器
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有后台接口（比如 /admin 开头）
                .addPathPatterns("/admin/**")
                // 放行登录接口和跨域配置
                .excludePathPatterns("/api/login", "/api/register");
    }
}