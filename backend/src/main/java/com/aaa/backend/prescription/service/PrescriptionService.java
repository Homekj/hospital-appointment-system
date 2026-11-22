package com.aaa.backend.prescription.service;

import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.entity.BalanceLog;
import com.aaa.backend.entity.UserNotification;
import com.aaa.backend.notification.dao.UserNotificationDao;
import com.aaa.backend.prescription.dao.PrescriptionDao;
import com.aaa.backend.prescription.dao.PrescriptionItemDao;
import com.aaa.backend.prescription.vo.PrescriptionDetailVO;
import com.aaa.backend.wallet.dao.BalanceLogDao;
import com.aaa.backend.wallet.dao.WalletUserDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 患者处方业务（docs/api.md 9）。
 *
 * 支付与挂号费支付是两套独立事务：处方支付只改 prescription 状态并写余额流水，
 * 按契约「预约保持 pending_rx_pay」，所以这里刻意不触碰 appointment 表。
 */
@Slf4j
@Service
public class PrescriptionService {

    private final PrescriptionDao prescriptionDao;
    private final PrescriptionItemDao prescriptionItemDao;
    private final WalletUserDao walletUserDao;
    private final BalanceLogDao balanceLogDao;
    private final UserNotificationDao notificationDao;

    public PrescriptionService(PrescriptionDao prescriptionDao,
                               PrescriptionItemDao prescriptionItemDao,
                               WalletUserDao walletUserDao,
                               BalanceLogDao balanceLogDao,
                               UserNotificationDao notificationDao) {
        this.prescriptionDao = prescriptionDao;
        this.prescriptionItemDao = prescriptionItemDao;
        this.walletUserDao = walletUserDao;
        this.balanceLogDao = balanceLogDao;
        this.notificationDao = notificationDao;
    }

    /** 按预约查处方：SQL 已带 user_id，天然完成归属校验。 */
    public PrescriptionDetailVO byAppointment(Long userId, Long appointmentId) {
        PrescriptionDetailVO vo = prescriptionDao.selectByAppointment(appointmentId, userId);
        if (vo == null) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "暂无处方");
        }
        return decorate(vo);
    }

    public PrescriptionDetailVO byId(Long userId, Long prescriptionId) {
        return decorate(requireOwned(userId, prescriptionId));
    }

    /**
     * 支付处方：仅 unpaid 可支付；条件更新 + 余额原子扣减 + 流水同事务。
     * 重复请求在状态与支付方式一致时幂等返回当前处方，绝不重复扣款。
     */
    @Transactional(rollbackFor = Exception.class)
    public PrescriptionDetailVO pay(Long userId, Long prescriptionId, String payMethod) {
        if (prescriptionDao.lockOwnedId(prescriptionId, userId) == null) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "暂无处方");
        }
        PrescriptionDetailVO current = requireOwned(userId, prescriptionId);
        if (!"unpaid".equals(current.getStatus())) {
            if (isAlreadyPaid(current, payMethod)) {
                return decorate(current); // 幂等
            }
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE,
                    "当前处方状态不允许支付，或已使用其他方式支付");
        }

        int rows = prescriptionDao.markPaid(prescriptionId, userId, payMethod);
        if (rows == 0) {
            PrescriptionDetailVO refreshed = requireOwned(userId, prescriptionId);
            if (isAlreadyPaid(refreshed, payMethod)) {
                return decorate(refreshed);
            }
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT);
        }

        if ("balance".equals(payMethod)) {
            BigDecimal amount = current.getTotalAmount() == null ? BigDecimal.ZERO : current.getTotalAmount();
            int deducted = walletUserDao.deductBalance(userId, amount);
            if (deducted == 0) {
                // 抛异常让 markPaid 一并回滚，不允许「处方已支付但没扣钱」
                throw new BusinessException(BusinessErrorCode.CONFLICT, "余额不足，请先充值");
            }
            BigDecimal after = walletUserDao.selectBalance(userId);
            BalanceLog logEntry = new BalanceLog();
            logEntry.setUserId(userId);
            logEntry.setType("prescription_pay");
            logEntry.setAmount(amount.negate());
            logEntry.setBalanceAfter(after);
            logEntry.setOrderId(prescriptionId);
            logEntry.setPayMethod("balance");
            logEntry.setRemark("处方费用支付");
            balanceLogDao.insert(logEntry);
        }
        notifyQuietly(userId, prescriptionId);
        return decorate(requireOwned(userId, prescriptionId));
    }

    /** 已发药的旧客户端可继续查询确认结果，发药动作由药房后台执行。 */
    public PrescriptionDetailVO pickup(Long userId, Long prescriptionId) {
        PrescriptionDetailVO prescription = requireOwned(userId, prescriptionId);
        if (!"dispensed".equals(prescription.getStatus())) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE,
                    "请到药房取药，由工作人员确认发药");
        }
        return decorate(prescription);
    }

    // ------------------------------------------------------------------ 私有辅助

    private static boolean isAlreadyPaid(PrescriptionDetailVO detail, String payMethod) {
        return ("paid".equals(detail.getStatus()) || "dispensed".equals(detail.getStatus()))
                && payMethod.equals(detail.getPayMethod());
    }

    private PrescriptionDetailVO requireOwned(Long userId, Long prescriptionId) {
        PrescriptionDetailVO vo = prescriptionDao.selectOwned(prescriptionId, userId);
        if (vo == null) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "暂无处方");
        }
        return vo;
    }

    private PrescriptionDetailVO decorate(PrescriptionDetailVO vo) {
        vo.setPayMethodText(payMethodText(vo.getPayMethod()));
        vo.setItems(prescriptionItemDao.selectByPrescription(vo.getId()));
        return vo;
    }

    private static String payMethodText(String payMethod) {
        if (payMethod == null) {
            return null;
        }
        return switch (payMethod) {
            case "balance" -> "余额支付";
            case "wechat" -> "微信支付";
            case "alipay" -> "支付宝支付";
            default -> payMethod;
        };
    }

    private void notifyQuietly(Long userId, Long prescriptionId) {
        try {
            UserNotification notification = new UserNotification();
            notification.setUserId(userId);
            notification.setType("prescription_paid");
            notification.setTitle("处方支付成功");
            notification.setContent("处方费用已支付，请按药房提示取药");
            notification.setBizType("prescription");
            notification.setBizId(prescriptionId);
            notification.setReadFlag(0);
            notificationDao.insert(notification);
        } catch (Exception exception) {
            log.warn("处方支付通知写入失败（不影响核心事务）: prescriptionId={}", prescriptionId, exception);
        }
    }
}
