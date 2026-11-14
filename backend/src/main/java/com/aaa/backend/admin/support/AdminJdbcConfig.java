package com.aaa.backend.admin.support;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 管理端只读统计 / 日志查询使用 JdbcTemplate。
 *
 * <p>Spring Boot 通常会自动配置 JdbcTemplate；这里用 {@code @ConditionalOnMissingBean}
 * 做兜底，保证在不同 Boot 版本下都只有一个同类型 Bean，避免按类型注入时出现歧义。
 */
@Configuration
public class AdminJdbcConfig {

    @Bean(name = "jdbcTemplate")
    @ConditionalOnMissingBean(JdbcTemplate.class)
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
