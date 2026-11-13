package com.aaa.backend.admin.log;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作日志落库。
 *
 * <p>使用 REQUIRES_NEW 开启独立事务：业务方法失败回滚、或业务返回业务错误时，
 * 审计日志仍能提交，不会出现「操作明明失败却没有失败记录」的情况。
 * 日志表只写不读业务数据，字段长度按 sql/system.sql 的 DDL 由切面负责截断。
 */
@Service
public class OperationLogWriter {

    private static final String INSERT_SQL = "INSERT INTO sys_operation_log "
            + "(username, module, action, method, url, ip, cost_ms, status) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public OperationLogWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(String username, String module, String action, String method,
                      String url, String ip, int costMs, int status) {
        jdbcTemplate.update(INSERT_SQL, username, module, action, method, url, ip, costMs, status);
    }
}
