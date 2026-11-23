package com.aaa.backend.appointment.service;

import com.aaa.backend.appointment.dao.AppointmentContractDao;
import com.aaa.backend.appointment.dao.AppointmentDao;
import com.aaa.backend.appointment.vo.AppointmentDetailVO;
import com.aaa.backend.appointment.vo.AppointmentSummaryRow;
import com.aaa.backend.catalog.dao.ScheduleDao;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.contract.appointment.AppointmentCommandService;
import com.aaa.backend.contract.appointment.AppointmentPageQuery;
import com.aaa.backend.contract.appointment.AppointmentQueryService;
import com.aaa.backend.contract.appointment.AppointmentSummary;
import com.aaa.backend.entity.BalanceLog;
import com.aaa.backend.entity.Schedule;
import com.aaa.backend.entity.UserNotification;
import com.aaa.backend.entity.enums.AppointmentStatus;
import com.aaa.backend.notification.dao.UserNotificationDao;
import com.aaa.backend.wallet.dao.BalanceLogDao;
import com.aaa.backend.wallet.dao.WalletUserDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * appointment 域对 admin / doctor 域暴露的契约实现。
 *
 * contract 包里只有接口、没有实现类，其他成员注入 AppointmentQueryService /
 * AppointmentCommandService 时会报 NoSuchBeanDefinitionException，本类就是补上这个缺口。
 * 实现类放在 appointment 模块（成员 A 负责目录），不修改 contract、admin、doctor 任何文件。
 */
@Slf4j
@Service
public class AppointmentContractService implements AppointmentQueryService, AppointmentCommandService {

    private final AppointmentContractDao contractDao;
    private final AppointmentDao appointmentDao;
    private final ScheduleDao scheduleDao;
    private final WalletUserDao walletUserDao;
    private final BalanceLogDao balanceLogDao;
    private final UserNotificationDao notificationDao;

    public AppointmentContractService(AppointmentContractDao contractDao,
                                      AppointmentDao appointmentDao,
                                      ScheduleDao scheduleDao,
                                      WalletUserDao walletUserDao,
                                      BalanceLogDao balanceLogDao,
                                      UserNotificationDao notificationDao) {
        this.contractDao = contractDao;
        this.appointmentDao = appointmentDao;
        this.scheduleDao = scheduleDao;
        this.walletUserDao = walletUserDao;
        this.balanceLogDao = balanceLogDao;
        this.notificationDao = notificationDao;
    }

    // ------------------------------------------------------------ 查询契约

    @Override
    public PageResult<AppointmentSummary> pageForAdmin(AppointmentPageQuery query) {
        return page(null, query);
    }

    /** 强制以入参 doctorId 覆盖 query.doctorId，医生不能通过请求参数查看别的医生号源。 */
    @Override
    public PageResult<AppointmentSummary> pageForDoctor(Long doctorId, AppointmentPageQuery query) {
        if (doctorId == null) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "缺少医生身份信息");
        }
        return page(doctorId, query);
    }

    @Override
    public AppointmentSummary getDetailForAdmin(Long appointmentId) {
        return requireSummary(contractDao.selectDetail(appointmentId, null));
    }

    @Override
    public AppointmentSummary getDetailForDoctor(Long appointmentId, Long doctorId) {
        if (doctorId == null) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "缺少医生身份信息");
        }
        return requireSummary(contractDao.selectDetail(appointmentId, doctorId));
    }

    private PageResult<AppointmentSummary> page(Long fixedDoctorId, AppointmentPageQuery raw) {
        AppointmentPageQuery query = raw == null ? new AppointmentPageQuery(null, null, null, null, null,
                null, null, null, null) : raw;
        List<String> statuses = statusFilter(query.status());
        long current = query.current();
        int size = query.size();
        long total = contractDao.countPage(statuses, fixedDoctorId != null ? fixedDoctorId : query.doctorId(),
                query.departmentId(), trimToNull(query.patientName()), query.startDate(),
                query.endDate(), query.scheduleDate());
        List<AppointmentSummary> records = total == 0 ? List.of()
                : contractDao.selectPage(statuses, fixedDoctorId != null ? fixedDoctorId : query.doctorId(),
                        query.departmentId(), trimToNull(query.patientName()), query.startDate(),
                        query.endDate(), query.scheduleDate(), (current - 1L) * size, size)
                .stream().map(AppointmentContractService::toSummary).toList();
        return new PageResult<>(records, total, current, size);
    }

    private static AppointmentSummary requireSummary(AppointmentSummaryRow row) {
        if (row == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND,
                    "预约不存在或不属于当前医生");
        }
        return toSummary(row);
    }

    /** 显式按字段顺序构造 record，不依赖 MyBatis 的构造器自动映射。 */
    private static AppointmentSummary toSummary(AppointmentSummaryRow row) {
        return new AppointmentSummary(
                row.getId(), row.getUserId(), row.getScheduleId(), row.getDoctorId(),
                row.getDoctorName(), row.getDoctorTitle(), row.getDepartmentId(), row.getDepartmentName(),
                row.getScheduleDate(), row.getTimeSlot(), row.getPatientName(), row.getPatientPhone(),
                row.getAmount(), row.getPayMethod(), row.getPayTime(), row.getStatus(), row.getRemark(),
                row.getCreateTime(), row.getUpdateTime());
    }

    /** pending 兼容历史别名 confirmed；其余状态按契约枚举取值精确匹配。 */
    private static List<String> statusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim();
        return switch (value) {
            case "unpaid", "pending", "confirmed", "checked_in", "in_consultation",
                 "pending_rx_pay", "completed", "cancelled" ->
                    "pending".equals(value) ? List.of("pending", "confirmed") : List.of(value);
            default -> throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "预约状态筛选值不合法");
        };
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // ------------------------------------------------------------ 写契约

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void cancelByAdmin(Long appointmentId, Long operatorUserId) {
        scheduleDao.selectByAppointmentForUpdate(appointmentId, null);
        if (contractDao.lockStatusForUpdate(appointmentId) == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND);
        }
        AppointmentDetailVO appointment = appointmentDao.selectDetail(appointmentId);
        if (appointment == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND);
        }
        AppointmentStatus status = AppointmentStatus.fromDatabaseValue(appointment.getStatus());
        if (status == AppointmentStatus.CANCELLED) {
            return;
        }
        if (!status.canCancelByAdmin()) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE, "当前预约状态不允许管理员取消");
        }
        closeByAdmin(appointment, "admin_cancelled", "预约已被管理员取消");
        log.info("管理员取消预约: appointmentId={}, operatorUserId={}", appointmentId, operatorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public int stopSchedule(Long scheduleId, String reason, Long operatorUserId) {
        Schedule schedule = scheduleDao.selectByIdForUpdate(scheduleId);
        if (schedule == null) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "排班不存在");
        }
        scheduleDao.markStopped(scheduleId);
        List<AppointmentDetailVO> waiting = contractDao.lockWaitingForSchedule(scheduleId);
        for (AppointmentDetailVO appointment : waiting) {
            closeByAdmin(appointment, "schedule_stopped", reason.strip());
        }
        log.info("管理员停诊: scheduleId={}, operatorUserId={}, cancelledCount={}", scheduleId, operatorUserId, waiting.size());
        return waiting.size();
    }

    private void closeByAdmin(AppointmentDetailVO appointment, String type, String reason) {
        String refundStatus = refundStatusFor(appointment);
        Long id = appointment.getId();
        if (contractDao.cancelByAdmin(id, type, reason, refundStatus) != 1) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT);
        }
        scheduleDao.releaseBookedCount(appointment.getScheduleId());
        if ("balance_refunded".equals(refundStatus)) {
            returnRegistrationBalance(appointment);
        }
        String refundMessage = switch (refundStatus) {
            case "balance_refunded" -> "挂号费已退回账户余额。";
            case "mock_refunded" -> "挂号费已登记模拟退款。";
            case "contact_hospital" -> "请联系医院确认挂号费处理方式。";
            default -> "预约未支付，无需退款。";
        };
        boolean stopped = "schedule_stopped".equals(type);
        String message = (stopped ? "停诊原因：" + reason + "。" : "")
                + refundMessage + "如需就诊，请从预约详情重新选号。";
        if (stopped) {
            insertNotification(appointment.getUserId(), "appointment_schedule_stopped", "停诊通知", message, id);
        } else {
            notifyQuietly(appointment.getUserId(), "appointment_cancelled", reason, message, id);
        }
    }

    private static String refundStatusFor(AppointmentDetailVO appointment) {
        if (AppointmentStatus.fromDatabaseValue(appointment.getStatus()) == AppointmentStatus.UNPAID) {
            return "not_required";
        }
        String method = appointment.getPayMethod();
        if ("balance".equals(method)) return "balance_refunded";
        if ("wechat".equals(method) || "alipay".equals(method)) return "mock_refunded";
        return "contact_hospital";
    }

    private void returnRegistrationBalance(AppointmentDetailVO appointment) {
        BigDecimal amount = appointment.getAmount();
        Long userId = appointment.getUserId();
        if (amount == null || amount.signum() < 0 || walletUserDao.addBalance(userId, amount) != 1) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "挂号费或余额账户异常，取消未完成");
        }
        BalanceLog entry = new BalanceLog();
        entry.setUserId(userId);
        entry.setType("order_refund");
        entry.setAmount(amount);
        entry.setBalanceAfter(walletUserDao.selectBalance(userId));
        entry.setOrderId(appointment.getId());
        entry.setPayMethod("balance");
        entry.setRemark("管理员取消预约，挂号费退回余额");
        balanceLogDao.insert(entry);
    }

    /** 开始接诊：checked_in -> in_consultation，重复调用幂等。 */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void startConsultation(Long appointmentId, Long doctorId) {
        scheduleDao.selectByAppointmentForUpdate(appointmentId, null);
        if (contractDao.lockStatusForUpdate(appointmentId) == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND,
                    "预约不存在或不属于当前医生");
        }
        AppointmentSummaryRow row = contractDao.selectDetail(appointmentId, doctorId);
        if (row == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND,
                    "预约不存在或不属于当前医生");
        }
        AppointmentStatus status = AppointmentStatus.fromDatabaseValue(row.getStatus());
        if (status == AppointmentStatus.IN_CONSULTATION) {
            return;
        }
        if (!status.canStartConsultation()) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE,
                    "仅已签到的预约可开始接诊");
        }
        int rows = contractDao.startConsultation(appointmentId, doctorId);
        if (rows == 0) {
            AppointmentSummaryRow refreshed = contractDao.selectDetail(appointmentId, doctorId);
            if (refreshed != null
                    && AppointmentStatus.fromDatabaseValue(refreshed.getStatus())
                    == AppointmentStatus.IN_CONSULTATION) {
                return;
            }
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT);
        }
        notifyQuietly(row.getUserId(), "doctor_called", "医生已叫号", "请查看预约详情，并按现场指引前往诊室。", appointmentId);
    }

    /**
     * 完成接诊：要求已有病历；有处方则进入 pending_rx_pay，无处方直接 completed。
     * 目标状态由 SQL 里的 CASE 决定，避免「先查再改」在并发下被插入处方打断。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeConsultation(Long appointmentId, Long doctorId) {
        if (contractDao.lockStatusForUpdate(appointmentId) == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND,
                    "预约不存在或不属于当前医生");
        }
        AppointmentSummaryRow row = contractDao.selectDetail(appointmentId, doctorId);
        if (row == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND,
                    "预约不存在或不属于当前医生");
        }
        AppointmentStatus status = AppointmentStatus.fromDatabaseValue(row.getStatus());
        if (status == AppointmentStatus.COMPLETED || status == AppointmentStatus.PENDING_RX_PAY) {
            return; // 重复完成幂等
        }
        if (!status.canCompleteConsultation()) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE,
                    "仅就诊中的预约可完成接诊");
        }
        if (contractDao.countConsultation(appointmentId) == 0) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "请先填写病历再完成接诊");
        }
        int rows = contractDao.completeConsultation(appointmentId, doctorId);
        if (rows == 0) {
            AppointmentSummaryRow refreshed = contractDao.selectDetail(appointmentId, doctorId);
            if (refreshed != null) {
                AppointmentStatus refreshedStatus = AppointmentStatus.fromDatabaseValue(refreshed.getStatus());
                if (refreshedStatus == AppointmentStatus.COMPLETED
                        || refreshedStatus == AppointmentStatus.PENDING_RX_PAY) {
                    return;
                }
            }
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeAfterPrescriptionDispense(Long appointmentId) {
        completeAfterPrescriptionAction(appointmentId, "dispensed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeAfterPrescriptionCancellation(Long appointmentId) {
        completeAfterPrescriptionAction(appointmentId, "cancelled");
    }

    private void completeAfterPrescriptionAction(Long appointmentId, String prescriptionStatus) {
        if (contractDao.lockStatusForUpdate(appointmentId) == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND, "预约不存在");
        }
        AppointmentSummaryRow row = contractDao.selectDetail(appointmentId, null);
        if (row == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND, "预约不存在");
        }
        AppointmentStatus status = AppointmentStatus.fromDatabaseValue(row.getStatus());
        if (status == AppointmentStatus.COMPLETED) {
            return;
        }
        if (status != AppointmentStatus.PENDING_RX_PAY) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE,
                    "当前预约不在处方处理阶段");
        }
        int rows = contractDao.completeAfterPrescriptionAction(appointmentId, prescriptionStatus);
        if (rows == 0) {
            AppointmentSummaryRow refreshed = contractDao.selectDetail(appointmentId, null);
            if (refreshed != null
                    && AppointmentStatus.fromDatabaseValue(refreshed.getStatus()) == AppointmentStatus.COMPLETED) {
                return;
            }
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT,
                    "预约状态已变化，请刷新后重试");
        }
    }

    /** 站内通知属于附属动作，失败不得回滚上述状态流转。 */
    private void notifyQuietly(Long userId, String type, String title, String content, Long bizId) {
        try {
            insertNotification(userId, type, title, content, bizId);
        } catch (Exception exception) {
            log.warn("业务通知写入失败（不影响核心事务）: bizId={}", bizId, exception);
        }
    }

    private void insertNotification(Long userId, String type, String title, String content, Long bizId) {
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBizType("appointment");
        notification.setBizId(bizId);
        notification.setReadFlag(0);
        notificationDao.insert(notification);
    }
}
