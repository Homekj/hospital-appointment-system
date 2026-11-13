package com.aaa.backend.wallet.service;

import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.entity.BalanceLog;
import com.aaa.backend.wallet.dao.BalanceLogDao;
import com.aaa.backend.wallet.dao.WalletUserDao;
import com.aaa.backend.wallet.dto.RechargeRequest;
import com.aaa.backend.wallet.vo.BalanceLogVO;
import com.aaa.backend.wallet.vo.RechargeResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class WalletService {

    private static final BigDecimal MIN_RECHARGE = new BigDecimal("0.01");
    private static final BigDecimal MAX_RECHARGE = new BigDecimal("10000.00");

    private final WalletUserDao walletUserDao;
    private final BalanceLogDao balanceLogDao;

    public WalletService(WalletUserDao walletUserDao, BalanceLogDao balanceLogDao) {
        this.walletUserDao = walletUserDao;
        this.balanceLogDao = balanceLogDao;
    }

    /** 原子加款 + 写流水，两步同事务；金额统一保留两位小数。 */
    @Transactional(rollbackFor = Exception.class)
    public RechargeResultVO recharge(Long userId, RechargeRequest request) {
        BigDecimal amount = request.amount();
        if (amount.compareTo(MIN_RECHARGE) < 0 || amount.compareTo(MAX_RECHARGE) > 0) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST,
                    "充值金额必须在 0.01 到 10000.00 之间");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        int rows = walletUserDao.addBalance(userId, amount);
        if (rows == 0) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "用户不存在");
        }
        BigDecimal after = walletUserDao.selectBalance(userId);

        BalanceLog logEntry = new BalanceLog();
        logEntry.setUserId(userId);
        logEntry.setType("recharge");
        logEntry.setAmount(amount);
        logEntry.setBalanceAfter(after);
        logEntry.setPayMethod(request.payMethod());
        logEntry.setRemark("余额充值");
        balanceLogDao.insert(logEntry);

        return new RechargeResultVO(after, logEntry.getId());
    }

    /** 按 createTime 倒序分页展示本人流水。 */
    public PageResult<BalanceLogVO> logs(Long userId, long current, int size) {
        long total = balanceLogDao.countLogs(userId);
        List<BalanceLogVO> records = total == 0
                ? List.of()
                : balanceLogDao.selectLogs(userId, (current - 1L) * size, size);
        return new PageResult<>(records, total, current, size);
    }
}
