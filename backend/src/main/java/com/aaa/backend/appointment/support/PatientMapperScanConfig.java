package com.aaa.backend.appointment.support;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * 患者业务模块自己的 Mapper 扫描入口。
 *
 * 启动类 BackendApplication 上的 @MapperScan 只覆盖 com.aaa.backend.dao（登录模块使用）。
 * 本模块禁止修改 config 包，因此在自己的业务包内补一条扫描规则：
 * 所有新 Mapper 都留在成员 A 负责目录下，也避免与其他成员的同名 Dao 冲突。
 * 两个 @MapperScan 由不同配置类承载，生成的注册器 bean 名不同，互不覆盖。
 */
@Configuration
@MapperScan(basePackages = {
        "com.aaa.backend.catalog.dao",
        "com.aaa.backend.appointment.dao",
        "com.aaa.backend.notification.dao",
        "com.aaa.backend.wallet.dao",
        "com.aaa.backend.prescription.dao",
        "com.aaa.backend.doctor.dao"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class PatientMapperScanConfig {
}
