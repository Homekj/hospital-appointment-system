package com.aaa.backend.appointment.service;

import com.aaa.backend.appointment.dao.AppointmentDao;
import com.aaa.backend.appointment.dto.CreateAppointmentRequest;
import com.aaa.backend.appointment.vo.AppointmentDetailVO;
import com.aaa.backend.catalog.dao.ScheduleDao;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.entity.Appointment;
import com.aaa.backend.entity.BalanceLog;
import com.aaa.backend.entity.Schedule;
import com.aaa.backend.entity.UserNotification;
import com.aaa.backend.entity.enums.AppointmentStatus;
import com.aaa.backend.notification.dao.UserNotificationDao;
import com.aaa.backend.wallet.dao.BalanceLogDao;
import com.aaa.backend.wallet.dao.WalletUserDao;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 患者预约业务。所有写方法都带 @Transactional，
 * 号源扣减、预约插入、余额扣减、流水、状态流转共享同一个数据库事务。
 */
@Slf4j
@Service
public class AppointmentService {

    private static final BigDecimal DEFAULT_REGISTRATION_FEE = new BigDecimal("20.00");

    private static final Set<String> ALLOWED_FILTER_STATUS = Set.of(
            "unpaid", "pending", "checked_in", "in_consultation",
            "pending_rx_pay", "pending_dispense", "completed", "cancelled");

    private final AppointmentDao appointmentDao;
    private final ScheduleDao scheduleDao;
    private final WalletUserDao walletUserDao;
    private final BalanceLogDao balanceLogDao;
    private final UserNotificationDao notificationDao;
    private final PatientProfileService patientProfiles;

    /** 是否强制「就诊当天才能签到」；联调时可在 application.properties 配 false 放宽。 */
    @Value("${patient.check-in.same-day-only:true}")
    private boolean sameDayCheckInOnly;

    @Value("${patient.appointment.payment-hold-minutes:15}")
    private int paymentHoldMinutes = 15;

    @PostConstruct
    void validatePaymentHold() {
        if (paymentHoldMinutes < 1 || paymentHoldMinutes > 1440) {
            throw new IllegalArgumentException("patient.appointment.payment-hold-minutes 必须在 1 到 1440 之间");
        }
    }

    public AppointmentService(AppointmentDao appointmentDao,
                              ScheduleDao scheduleDao,
                              WalletUserDao walletUserDao,
                              BalanceLogDao balanceLogDao,
                              UserNotificationDao notificationDao, PatientProfileService patientProfiles) {
        this.appointmentDao = appointmentDao;
        this.scheduleDao = scheduleDao;
        this.walletUserDao = walletUserDao;
        this.balanceLogDao = balanceLogDao;
        this.notificationDao = notificationDao;
        this.patientProfiles = patientProfiles;
    }

    // ------------------------------------------------------------------ 查询

    /** 我的预约：强制 user_id = JWT 用户，支持派生筛选 pending_dispense 与历史别名 confirmed。 */
    public PageResult<AppointmentDetailVO> myAppointments(Long userId, long current, int size, String status) {
        List<String> statuses = null;
        boolean pendingDispense = false;
        if (status != null && !status.isBlank()) {
            String filter = status.trim();
            if (!ALLOWED_FILTER_STATUS.contains(filter)) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "预约状态筛选值不合法");
            }
            switch (filter) {
                case "pending" -> statuses = List.of("pending", "confirmed");
                case "pending_dispense" -> {
                    statuses = List.of("pending_rx_pay");
                    pendingDispense = true;
                }
                default -> statuses = List.of(filter);
            }
        }
        long total = appointmentDao.countMyPage(userId, statuses, pendingDispense);
        List<AppointmentDetailVO> records = total == 0
                ? List.of()
                : appointmentDao.selectMyPage(userId, statuses, pendingDispense,
                        (current - 1L) * size, size);
        records.forEach(vo -> decorate(vo, false));
        return new PageResult<>(records, total, current, size);
    }

    @Transactional(readOnly = true)
    public AppointmentDetailVO detail(Long userId, Long appointmentId) {
        AppointmentDetailVO detail = requireOwned(userId, appointmentId);
        AppointmentStatus status = AppointmentStatus.fromDatabaseValue(detail.getStatus());
        if (status == AppointmentStatus.CHECKED_IN || status == AppointmentStatus.IN_CONSULTATION) {
            AppointmentDetailVO queue = appointmentDao.queueDetails(appointmentId, userId);
            if (queue != null) {
                detail.setCheckInTime(queue.getCheckInTime());
                detail.setWaitingAhead(queue.getWaitingAhead());
                detail.setInConsultationCount(queue.getInConsultationCount());
            }
        }
        return detail;
    }

    // ------------------------------------------------------------------ 写操作

    /** 创建预约：号源扣减与预约插入必须同时成功或同时回滚。 */
    @Transactional(rollbackFor = Exception.class)
    public AppointmentDetailVO create(Long userId, CreateAppointmentRequest request) {
        // This locking read is intentionally the first database read in the transaction.
        // It serializes duplicate booking checks for the same user and schedule even under
        // MySQL REPEATABLE READ, then the quota update remains the atomic capacity guard.
        Schedule schedule = scheduleDao.selectByIdForUpdate(request.scheduleId());
        if (schedule == null) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "排班不存在");
        }
        if (!Integer.valueOf(1).equals(schedule.getStatus())) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "该排班已停诊");
        }
        if (schedule.getScheduleDate() == null || schedule.getScheduleDate().isBefore(LocalDate.now())) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "该排班日期已过，无法预约");
        }
        BigDecimal registrationFee = Objects.requireNonNullElse(schedule.getRegistrationFee(), DEFAULT_REGISTRATION_FEE);
        if (request.expectedRegistrationFee() != null
                && registrationFee.compareTo(request.expectedRegistrationFee()) != 0) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "挂号费已调整，请重新确认排班和费用");
        }
        Long profileId = request.patientProfileId();
        PatientProfileService.Profile profile = profileId == null ? null : patientProfiles.require(userId, profileId);
        String name = profile == null ? request.patientName().strip() : profile.name();
        String phone = profile == null ? request.patientPhone() : profile.phone();
        if (appointmentDao.countActiveBySchedule(userId, request.scheduleId(), profileId, name, phone) > 0) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "该就诊人已预约此时段，请在我的预约中查看");
        }

        // 关键：一条带条件的原子 UPDATE；影响行数 0 说明号源已满或排班被停用
        int quotaRows = scheduleDao.increaseBookedCount(request.scheduleId());
        if (quotaRows == 0) {
            throw new BusinessException(BusinessErrorCode.SCHEDULE_FULL); // 409 该时段号源已满
        }

        // 与上面的扣号源处于同一事务；插入失败会连带把号源回滚
        Appointment entity = new Appointment();
        entity.setUid(userId);
        entity.setScId(request.scheduleId());
        entity.setPatientProfileId(profileId);
        entity.setPatientName(name);
        entity.setPatientPhone(phone);
        entity.setIdCard(request.idCard());
        entity.setRemark(request.remark());
        entity.setAmount(registrationFee.doubleValue());
        entity.setStatus(AppointmentStatus.UNPAID.value());
        entity.setPaymentDeadline(appointmentDao.newPaymentDeadline(paymentHoldMinutes));
        appointmentDao.insert(entity);

        notifyQuietly(userId, "appointment_success", "号源已保留",
                "请在 " + paymentHoldMinutes + " 分钟内支付挂号费，超时后号源将自动释放", entity.getApId());
        return requireOwned(userId, entity.getApId());
    }

    /** 支付：仅 unpaid 可支付；balance 支付的扣款、流水、状态更新在同一事务。 */
    @Transactional(rollbackFor = Exception.class)
    public AppointmentDetailVO pay(Long userId, Long appointmentId, String payMethod) {
        AppointmentDetailVO current = requireOwnedForUpdate(userId, appointmentId);
        AppointmentStatus status = AppointmentStatus.fromDatabaseValue(current.getStatus());

        // 幂等：相同方式重复支付直接返回当前预约，不再扣款
        if (status == AppointmentStatus.PENDING && payMethod.equals(current.getPayMethod())) {
            return current;
        }
        if ("payment_timeout".equals(current.getCancellationType()) || Boolean.TRUE.equals(current.getPaymentExpired())) {
            throw paymentTimeout();
        }
        if (!status.canPay()) {
            String message = status == AppointmentStatus.PENDING
                    ? "该预约已使用其他方式支付"
                    : "当前预约状态不允许支付";
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE, message);
        }

        // 条件更新抢状态：并发下只有一个请求能拿到影响行数 1，杜绝重复扣款
        int rows = appointmentDao.markPaid(appointmentId, userId, payMethod);
        if (rows == 0) {
            if (Boolean.TRUE.equals(appointmentDao.hasExpiredPaymentDeadline(appointmentId))) {
                throw paymentTimeout();
            }
            AppointmentDetailVO refreshed = requireOwned(userId, appointmentId);
            if (AppointmentStatus.fromDatabaseValue(refreshed.getStatus()) == AppointmentStatus.PENDING
                    && payMethod.equals(refreshed.getPayMethod())) {
                return refreshed;
            }
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT);
        }

        if ("balance".equals(payMethod)) {
            BigDecimal amount = amountOf(current.getAmount());
            int deducted = walletUserDao.deductBalance(userId, amount); // balance >= amount 才扣得动
            if (deducted == 0) {
                // 抛异常让 markPaid 一并回滚，绝不允许「已支付但没扣钱」
                throw new BusinessException(BusinessErrorCode.CONFLICT, "余额不足，请先充值");
            }
            BigDecimal after = walletUserDao.selectBalance(userId);
            insertBalanceLog(userId, "order_pay", amount.negate(), after,
                    appointmentId, "balance", "挂号费支付");
        }
        notifyQuietly(userId, "appointment_paid", "预约成功",
                "挂号费支付成功，请按时到院签到", appointmentId);
        return requireOwned(userId, appointmentId);
    }

    private static BusinessException paymentTimeout() {
        return new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT, "支付已超时，请重新预约");
    }

    @Transactional(rollbackFor = Exception.class)
    public void expireUnpaid(Long appointmentId) {
        AppointmentDetailVO locked = appointmentDao.lockByIdForUpdate(appointmentId);
        if (locked == null || appointmentDao.cancelExpiredPayment(appointmentId) != 1) {
            return;
        }
        scheduleDao.releaseBookedCount(locked.getScheduleId());
        notifyQuietly(locked.getUserId(), "appointment_payment_timeout", "预约已超时取消",
                "未在保留时间内完成支付，号源已释放。您可重新选择时段。", appointmentId);
    }

    /** 取消：条件更新防止重复取消；只有取消成功才释放号源、才退款。 */
    @Transactional(rollbackFor = Exception.class)
    public AppointmentDetailVO cancel(Long userId, Long appointmentId) {
        AppointmentDetailVO current = requireOwnedForUpdate(userId, appointmentId);
        AppointmentStatus status = AppointmentStatus.fromDatabaseValue(current.getStatus());
        if (status == AppointmentStatus.CANCELLED) {
            return current; // 重复取消幂等，不会二次退号
        }
        if (!status.canCancelByPatient()) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE,
                    "当前预约状态不允许患者自行取消");
        }

        int rows = appointmentDao.cancel(appointmentId, userId);
        if (rows == 0) {
            AppointmentDetailVO refreshed = requireOwned(userId, appointmentId);
            if (AppointmentStatus.fromDatabaseValue(refreshed.getStatus()) == AppointmentStatus.CANCELLED) {
                return refreshed;
            }
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT);
        }

        scheduleDao.releaseBookedCount(current.getScheduleId()); // GREATEST 保证不小于 0

        boolean balanceRefund = status == AppointmentStatus.PENDING
                && "balance".equals(current.getPayMethod());
        if (balanceRefund) {
            BigDecimal amount = amountOf(current.getAmount());
            walletUserDao.addBalance(userId, amount);
            BigDecimal after = walletUserDao.selectBalance(userId);
            insertBalanceLog(userId, "order_refund", amount, after,
                    appointmentId, "balance", "挂号费原路退回余额");
            notifyQuietly(userId, "appointment_cancelled", "预约已取消",
                    "挂号费已退回账户余额", appointmentId);
        } else {
            String note = status == AppointmentStatus.PENDING
                    ? "模拟支付退款已在演示环境登记，不调用真实退款网关"
                    : "未支付预约，不涉及退款";
            notifyQuietly(userId, "appointment_cancelled", "预约已取消", note, appointmentId);
        }
        return requireOwned(userId, appointmentId);
    }

    /** 签到：仅 pending 可签到，重复签到返回当前状态，不允许状态倒退。 */
    @Transactional(rollbackFor = Exception.class)
    public AppointmentDetailVO checkIn(Long userId, Long appointmentId) {
        AppointmentDetailVO current = requireOwnedForUpdate(userId, appointmentId);
        AppointmentStatus status = AppointmentStatus.fromDatabaseValue(current.getStatus());
        if (status == AppointmentStatus.CHECKED_IN) {
            return current;
        }
        if (!status.canCheckIn()) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE,
                    "仅已支付且未签到的预约可签到");
        }

        int rows = sameDayCheckInOnly
                ? appointmentDao.checkInOnScheduleDate(appointmentId, userId)
                : appointmentDao.checkIn(appointmentId, userId);
        if (rows == 0) {
            AppointmentDetailVO refreshed = requireOwned(userId, appointmentId);
            AppointmentStatus refreshedStatus = AppointmentStatus.fromDatabaseValue(refreshed.getStatus());
            if (refreshedStatus == AppointmentStatus.CHECKED_IN) {
                return refreshed;
            }
            if (sameDayCheckInOnly && refreshedStatus == AppointmentStatus.PENDING
                    && !LocalDate.now().equals(refreshed.getScheduleDate())) {
                throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE,
                        "仅可在就诊当天签到");
            }
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT);
        }
        notifyQuietly(userId, "check_in_success", "签到成功",
                "您已到院签到，请等待医生接诊", appointmentId);
        return requireOwned(userId, appointmentId);
    }

    // ------------------------------------------------------------------ 私有辅助

    /** 所有读写都先过这里：user_id 必须等于 JWT 里的当前用户，杜绝越权访问他人预约。 */
    private AppointmentDetailVO requireOwned(Long userId, Long appointmentId) {
        AppointmentDetailVO vo = appointmentDao.selectDetail(appointmentId);
        if (vo == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND);
        }
        if (!Objects.equals(userId, vo.getUserId())) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN);
        }
        decorate(vo, true);
        return vo;
    }

    /** Lock the appointment before state checks so concurrent retries observe committed transitions. */
    private AppointmentDetailVO requireOwnedForUpdate(Long userId, Long appointmentId) {
        AppointmentDetailVO locked = appointmentDao.lockByIdForUpdate(appointmentId);
        if (locked == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND);
        }
        if (!Objects.equals(userId, locked.getUserId())) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN);
        }
        return requireOwned(userId, appointmentId);
    }

    private void decorate(AppointmentDetailVO vo, boolean includeIdCard) {
        vo.setPayMethodText(payMethodText(vo.getPayMethod()));
        // 列表默认不返回身份证号；详情做脱敏，只保留前 3 位和后 4 位
        vo.setIdCard(includeIdCard ? maskIdCard(vo.getIdCard()) : null);
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

    private static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() <= 7) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    private static BigDecimal amountOf(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private void insertBalanceLog(Long userId, String type, BigDecimal amount, BigDecimal balanceAfter,
                                  Long orderId, String payMethod, String remark) {
        BalanceLog logEntry = new BalanceLog();
        logEntry.setUserId(userId);
        logEntry.setType(type);
        logEntry.setAmount(amount);
        logEntry.setBalanceAfter(balanceAfter);
        logEntry.setOrderId(orderId);
        logEntry.setPayMethod(payMethod);
        logEntry.setRemark(remark);
        // createTime 不赋值，交给数据库 DEFAULT CURRENT_TIMESTAMP
        balanceLogDao.insert(logEntry);
    }

    /**
     * 通知写入失败不得回滚核心挂号/支付事务（docs/api.md 16）。
     * 这里捕获异常只记日志；生产更严谨的做法是改成事务提交后的 AFTER_COMMIT 事件。
     */
    private void notifyQuietly(Long userId, String type, String title, String content, Long bizId) {
        try {
            UserNotification notification = new UserNotification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(content.length() > 500 ? content.substring(0, 500) : content);
            notification.setBizType("appointment");
            notification.setBizId(bizId);
            notification.setReadFlag(0);
            notificationDao.insert(notification);
        } catch (Exception exception) {
            log.warn("业务通知写入失败（不影响核心事务）: type={}, bizId={}", type, bizId, exception);
        }
    }
}
