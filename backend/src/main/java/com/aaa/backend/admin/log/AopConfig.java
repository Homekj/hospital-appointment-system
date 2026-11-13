package com.aaa.backend.admin.log;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 显式开启 AspectJ 自动代理。
 *
 * <p>Spring Boot 4 已移除 spring-boot-starter-aop，但 spring-context 自带 spring-aop，
 * 加上 classpath 中的 aspectjweaver 即可支持 @Aspect 环绕通知。
 * 这里显式声明，避免不同 Boot 版本的自动配置差异导致切面静默失效。
 */
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {
}
