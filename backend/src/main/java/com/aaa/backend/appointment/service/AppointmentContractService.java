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
import com.aaa.backend.entity.UserNotification;
import com.aaa.backend.entity.enums.AppointmentStatus;
import com.aaa.backend.notification.dao.UserNotificationDao;
import com.aaa.backend.wallet.dao.BalanceLogDao;
import com.aaa.backend.wallet.dao.WalletUserDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    /**
     * 管理员取消：条件更新抢状态，取消成功才退号源、才退款，全部在同一事务。
     * operatorUserId 只用于审计日志，不参与数据归属判断。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByAdmin(Long appointmentId, Long operatorUserId) {
        if (contractDao.lockStatusForUpdate(appointmentId) == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND);
        }
        AppointmentDetailVO vo = appointmentDao.selectDetail(appointmentId);
        if (vo == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND);
        }
        AppointmentStatus status = AppointmentStatus.fromDatabaseValue(vo.getStatus());
        if (status == AppointmentStatus.CANCELLED) {
            return; // 重复取消幂等，绝不二次退号 / 二次退款
        }
        if (!status.canCancelByAdmin()) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE,
                    "当前预约状态不允许管理员取消");
        }

        int rows = contractDao.cancelByAdmin(appointmentId);
        if (rows == 0) {
            AppointmentDetailVO refreshed = appointmentDao.selectDetail(appointmentId);
            if (refreshed != null
                    && AppointmentStatus.fromDatabaseValue(refreshed.getStatus()) == AppointmentStatus.CANCELLED) {
                return;
            }
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT);
        }

        scheduleDao.releaseBookedCount(vo.getScheduleId());
        if (status == AppointmentStatus.PENDING && "balance".equals(vo.getPayMethod())) {
            BigDecimal amount = vo.getAmount() == null ? BigDecimal.ZERO : vo.getAmount();
            walletUserDao.addBalance(vo.getUserId(), amount);
            BigDecimal after = walletUserDao.selectBalance(vo.getUserId());
            BalanceLog logEntry = new BalanceLog();
            logEntry.setUserId(vo.getUserId());
            logEntry.setType("order_refund");
            logEntry.setAmount(amount);
            logEntry.setBalanceAfter(after);
            logEntry.setOrderId(appointmentId);
            logEntry.setPayMethod("balance");
            logEntry.setRemark("管理员取消预约，挂号费原路退回余额");
            balanceLogDao.insert(logEntry);
        }
        log.info("管理员取消预约: appointmentId={}, operatorUserId={}", appointmentId, operatorUserId);
        notifyQuietly(vo.getUserId(), "预约已被管理员取消", "如需重新就诊请重新预约", appointmentId);
    }

    /** 开始接诊：checked_in -> in_consultation，重复调用幂等。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startConsultation(Long appointmentId, Long doctorId) {
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
    private void notifyQuietly(Long userId, String title, String content, Long bizId) {
        try {
            UserNotification notification = new UserNotification();
            notification.setUserId(userId);
            notification.setType("appointment_cancelled");
            notification.setTitle(title);
            notification.setContent(content);
            notification.setBizType("appointment");
            notification.setBizId(bizId);
            notification.setReadFlag(0);
            notificationDao.insert(notification);
        } catch (Exception exception) {
            log.warn("业务通知写入失败（不影响核心事务）: bizId={}", bizId, exception);
        }
    }
}
