package com.aaa.backend.auth;

import com.aaa.backend.dao.SysLoginLogDao;
import com.aaa.backend.entity.SysLoginLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Best-effort login auditing; credentials and third-party codes are never recorded. */
@Service
public class LoginAuditService {
    private static final Logger log = LoggerFactory.getLogger(LoginAuditService.class);
    private final SysLoginLogDao loginLogDao;

    public LoginAuditService(SysLoginLogDao loginLogDao) {
        this.loginLogDao = loginLogDao;
    }

    public void record(String username, String ip, boolean success, String message) {
        try {
            SysLoginLog entry = new SysLoginLog();
            entry.setUsername(limit(username, 50));
            entry.setIp(limit(ip, 50));
            entry.setStatus(success ? 1 : 0);
            entry.setMessage(limit(message, 200));
            loginLogDao.insert(entry);
        } catch (Exception exception) {
            log.warn("Unable to persist login audit record for username={}", limit(username, 50), exception);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
