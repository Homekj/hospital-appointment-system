package com.aaa.backend.admin.support;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/** Mapper registration owned by the admin module. */
@Configuration
@MapperScan(basePackages = "com.aaa.backend.admin.dao",
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class AdminMapperScanConfig {
}
