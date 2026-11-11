package com.aaa.backend.notification.service;

import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.notification.dao.UserNotificationDao;
import com.aaa.backend.notification.vo.NotificationVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final UserNotificationDao notificationDao;

    public NotificationService(UserNotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    /** 只查当前用户；read 为空时不过滤已读状态。 */
    public PageResult<NotificationVO> page(Long userId, long current, int size, Boolean read) {
        Integer readFlag = read == null ? null : (Boolean.TRUE.equals(read) ? 1 : 0);
        long total = notificationDao.countPage(userId, readFlag);
        List<NotificationVO> records = total == 0
                ? List.of()
                : notificationDao.selectPage(userId, readFlag, (current - 1L) * size, size);
        return new PageResult<>(records, total, current, size);
    }

    public long unreadCount(Long userId) {
        return notificationDao.countUnread(userId);
    }

    public void markRead(Long userId, Long id) {
        if (notificationDao.countOwned(id, userId) == 0) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "通知不存在");
        }
        // 条件更新：本来就已读时影响行数为 0，按幂等成功处理
        notificationDao.markRead(id, userId);
    }

    public long markAllRead(Long userId) {
        return notificationDao.markAllRead(userId);
    }
}
