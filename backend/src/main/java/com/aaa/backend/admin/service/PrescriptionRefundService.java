package com.aaa.backend.admin.service;

import com.aaa.backend.admin.dao.AdminPrescriptionDao;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.contract.appointment.AppointmentCommandService;
import com.aaa.backend.entity.BalanceLog;
import com.aaa.backend.entity.UserNotification;
import com.aaa.backend.notification.dao.UserNotificationDao;
import com.aaa.backend.prescription.vo.PrescriptionDetailVO;
import com.aaa.backend.wallet.dao.BalanceLogDao;
import com.aaa.backend.wallet.dao.WalletUserDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PrescriptionRefundService {
    private final AdminPrescriptionDao prescriptionDao;
    private final AdminOperationsService adminOperations;
    private final AppointmentCommandService appointments;
    private final WalletUserDao wallet;
    private final BalanceLogDao balanceLogs;
    private final UserNotificationDao notifications;

    @Transactional
    public PrescriptionDetailVO refund(Long id, String reason, Long operatorId) {
        if (prescriptionDao.lockForUpdate(id) == null) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "处方不存在");
        }
        PrescriptionDetailVO prescription = adminOperations.prescriptionDetail(id);
        if ("cancelled".equals(prescription.getStatus()) && prescription.getRefundTime() != null) {
            return prescription;
        }
        if (!"paid".equals(prescription.getStatus())) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE, "仅已支付且未发药的处方可以退费");
        }
        String payMethod = prescription.getPayMethod();
        boolean balancePayment = "balance".equals(payMethod);
        if (!balancePayment && !"wechat".equals(payMethod) && !"alipay".equals(payMethod)) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "无法确认该处方的原支付方式");
        }
        if (prescriptionDao.refundPaid(id, reason.strip(), operatorId) != 1) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT, "处方状态已变化，请刷新后重试");
        }

        if (balancePayment) {
            returnBalance(prescription);
        }
        appointments.completeAfterPrescriptionCancellation(prescription.getAppointmentId());
        notifyPatient(prescription, balancePayment);
        return adminOperations.prescriptionDetail(id);
    }

    private void returnBalance(PrescriptionDetailVO prescription) {
        BigDecimal amount = prescription.getTotalAmount();
        Long userId = prescription.getUserId();
        if (amount == null || amount.signum() < 0 || wallet.addBalance(userId, amount) != 1) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "处方金额或余额账户异常，退费未完成");
        }
        BalanceLog entry = new BalanceLog();
        entry.setType("prescription_refund");
        entry.setUserId(userId);
        entry.setAmount(amount);
        entry.setBalanceAfter(wallet.selectBalance(userId));
        entry.setOrderId(prescription.getId());
        entry.setPayMethod("balance");
        entry.setRemark("未发药处方退费");
        balanceLogs.insert(entry);
    }

    private void notifyPatient(PrescriptionDetailVO prescription, boolean balancePayment) {
        UserNotification notification = new UserNotification();
        notification.setUserId(prescription.getUserId());
        notification.setType("prescription_refunded");
        notification.setTitle("处方已退费");
        notification.setContent(balancePayment ? "药费已退回账户余额，请查看处方详情。"
                : "已登记模拟退款，请查看处方详情。");
        notification.setBizType("appointment");
        notification.setBizId(prescription.getAppointmentId());
        notification.setReadFlag(0);
        notifications.insert(notification);
    }
}
