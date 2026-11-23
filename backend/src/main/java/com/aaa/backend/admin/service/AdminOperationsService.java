package com.aaa.backend.admin.service;

import com.aaa.backend.admin.dao.AdminDataDao;
import com.aaa.backend.admin.dao.AdminMedicineDao;
import com.aaa.backend.admin.dao.AdminPrescriptionDao;
import com.aaa.backend.admin.dto.AdminLogPageQuery;
import com.aaa.backend.admin.dto.MedicineSaveRequest;
import com.aaa.backend.admin.vo.AdminUserVO;
import com.aaa.backend.admin.vo.DashboardStatsVO;
import com.aaa.backend.admin.vo.DashboardTrendVO;
import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.contract.appointment.AppointmentCommandService;
import com.aaa.backend.entity.Medicine;
import com.aaa.backend.entity.SysLoginLog;
import com.aaa.backend.entity.SysMenu;
import com.aaa.backend.entity.SysOperationLog;
import com.aaa.backend.prescription.dao.PrescriptionItemDao;
import com.aaa.backend.prescription.vo.PrescriptionDetailVO;
import com.aaa.backend.prescription.vo.PrescriptionItemVO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin P1 operations backed by domain DAOs and the appointment command contract. */
@Service
public class AdminOperationsService {
    private final AdminDataDao dataDao;
    private final AdminMedicineDao medicineDao;
    private final AdminPrescriptionDao prescriptionDao;
    private final PrescriptionItemDao prescriptionItemDao;
    private final AppointmentCommandService appointmentCommandService;
    private final CurrentUserContext currentUserContext;

    public AdminOperationsService(AdminDataDao dataDao,
                                  AdminMedicineDao medicineDao,
                                  AdminPrescriptionDao prescriptionDao,
                                  PrescriptionItemDao prescriptionItemDao,
                                  AppointmentCommandService appointmentCommandService,
                                  CurrentUserContext currentUserContext) {
        this.dataDao = dataDao;
        this.medicineDao = medicineDao;
        this.prescriptionDao = prescriptionDao;
        this.prescriptionItemDao = prescriptionItemDao;
        this.appointmentCommandService = appointmentCommandService;
        this.currentUserContext = currentUserContext;
    }

    public DashboardStatsVO dashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(6).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        Map<LocalDate, DashboardTrendVO> existing = new HashMap<>();
        for (DashboardTrendVO row : dataDao.selectDashboardTrend(start, end)) {
            existing.put(row.date(), row);
        }
        List<DashboardTrendVO> trend = new ArrayList<>(7);
        for (int day = 6; day >= 0; day--) {
            LocalDate date = today.minusDays(day);
            trend.add(existing.getOrDefault(date, new DashboardTrendVO(date, 0, BigDecimal.ZERO)));
        }
        BigDecimal todayRevenue = dataDao.sumTodayRevenue();
        return new DashboardStatsVO(
                dataDao.countTodayAppointments(),
                todayRevenue == null ? BigDecimal.ZERO : todayRevenue,
                dataDao.countPendingAppointments(),
                dataDao.countCompletedAppointments(),
                dataDao.countActiveDoctors(),
                dataDao.countActiveDepartments(),
                dataDao.countActiveUsers(),
                List.copyOf(trend));
    }

    public PageResult<PrescriptionDetailVO> prescriptions(String status, Long userId, Long doctorId,
                                                           long current, int size) {
        validatePage(current, size);
        String normalizedStatus = trimToNull(status);
        if (normalizedStatus != null && !List.of("unpaid", "paid", "dispensed", "cancelled").contains(normalizedStatus)) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "处方状态不合法");
        }
        long offset = (current - 1) * size;
        return new PageResult<>(prescriptionDao.selectPage(normalizedStatus, userId, doctorId, offset, size),
                prescriptionDao.countPage(normalizedStatus, userId, doctorId), current, size);
    }

    public PrescriptionDetailVO prescriptionDetail(Long id) {
        PrescriptionDetailVO detail = requirePrescription(id);
        detail.setItems(prescriptionItemDao.selectByPrescription(id));
        return detail;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public PrescriptionDetailVO dispense(Long id) {
        PrescriptionDetailVO prescription = requirePrescription(id);
        if ("dispensed".equals(prescription.getStatus())) {
            return prescriptionDetail(id);
        }
        if (!"paid".equals(prescription.getStatus())) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE, "仅已支付处方可以发药");
        }
        List<PrescriptionItemVO> items = prescriptionItemDao.selectByPrescription(id);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "处方没有药品明细，不能发药");
        }
        List<Long> medicineIds = items.stream().map(PrescriptionItemVO::getMedicineId)
                .filter(java.util.Objects::nonNull).distinct().sorted().toList();
        for (Long medicineId : medicineIds) {
            if (medicineDao.lockStockForUpdate(medicineId) == null) {
                throw new BusinessException(BusinessErrorCode.CONFLICT, "药品库存不足或药品已不存在");
            }
        }
        if (prescriptionDao.lockForUpdate(id) == null) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "处方不存在");
        }
        prescription = requirePrescription(id);
        if ("dispensed".equals(prescription.getStatus())) return prescriptionDetail(id);
        if (!"paid".equals(prescription.getStatus())) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE, "仅已支付处方可以发药");
        }
        items = prescriptionItemDao.selectByPrescription(id);
        for (PrescriptionItemVO item : items) {
            if (item.getMedicineId() == null || item.getQuantity() == null || item.getQuantity() <= 0
                    || medicineDao.deductStock(item.getMedicineId(), item.getQuantity()) == 0) {
                throw new BusinessException(BusinessErrorCode.CONFLICT, "药品库存不足或药品已不存在");
            }
        }
        if (prescriptionDao.markDispensed(id) == 0) {
            PrescriptionDetailVO latest = requirePrescription(id);
            if ("dispensed".equals(latest.getStatus())) {
                return prescriptionDetail(id);
            }
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT, "处方状态已变化，请刷新后重试");
        }
        appointmentCommandService.completeAfterPrescriptionDispense(prescription.getAppointmentId());
        return prescriptionDetail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public PrescriptionDetailVO cancelPrescription(Long id) {
        if (prescriptionDao.lockForUpdate(id) == null) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "处方不存在");
        }
        PrescriptionDetailVO prescription = requirePrescription(id);
        if ("cancelled".equals(prescription.getStatus())) {
            appointmentCommandService.completeAfterPrescriptionCancellation(prescription.getAppointmentId());
            return prescriptionDetail(id);
        }
        if (!"unpaid".equals(prescription.getStatus())) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE, "仅未支付处方可以取消");
        }
        if (prescriptionDao.cancelUnpaid(id) == 0) {
            PrescriptionDetailVO latest = requirePrescription(id);
            if (!"cancelled".equals(latest.getStatus())) {
                throw new BusinessException(BusinessErrorCode.APPOINTMENT_CONFLICT, "处方状态已变化，请刷新后重试");
            }
        }
        // This admin action closes the unpaid prescription stage and completes the already-finished visit.
        appointmentCommandService.completeAfterPrescriptionCancellation(prescription.getAppointmentId());
        return prescriptionDetail(id);
    }

    public PageResult<Medicine> medicines(String keyword, Integer status, long current, int size) {
        validatePage(current, size);
        validateStatus(status);
        String normalizedKeyword = trimToNull(keyword);
        long offset = (current - 1) * size;
        return new PageResult<>(medicineDao.selectAdminPage(normalizedKeyword, status, offset, size),
                medicineDao.countAdminPage(normalizedKeyword, status), current, size);
    }

    public Medicine medicineDetail(Long id) {
        Medicine medicine = medicineDao.selectById(id);
        if (medicine == null) throw new BusinessException(BusinessErrorCode.NOT_FOUND, "药品不存在");
        return medicine;
    }

    @Transactional(rollbackFor = Exception.class)
    public Medicine createMedicine(MedicineSaveRequest request) {
        Medicine medicine = request.toEntity();
        medicineDao.insert(medicine);
        return medicineDetail(medicine.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Medicine updateMedicine(Long id, MedicineSaveRequest request) {
        Integer lockedStock = medicineDao.lockStockForUpdate(id);
        if (lockedStock == null) throw new BusinessException(BusinessErrorCode.NOT_FOUND, "药品不存在");
        Medicine current = medicineDetail(id);
        Medicine updated = request.toEntity();
        int reserved = medicineDao.selectReservedQuantitiesForUpdate(id).stream()
                .filter(quantity -> quantity != null)
                .mapToInt(Integer::intValue)
                .sum();
        if (updated.getStock() != null && updated.getStock() < reserved) {
            throw new BusinessException(BusinessErrorCode.CONFLICT,
                    "库存不能低于未取药处方预留数量（" + reserved + "）");
        }
        if (request.getStatus() == null) updated.setStatus(current.getStatus());
        LambdaUpdateWrapper<Medicine> wrapper = new LambdaUpdateWrapper<Medicine>()
                .eq(Medicine::getId, id)
                .set(Medicine::getName, updated.getName())
                .set(Medicine::getSpec, updated.getSpec())
                .set(Medicine::getUnit, updated.getUnit())
                .set(Medicine::getPrice, updated.getPrice())
                .set(Medicine::getStock, updated.getStock())
                .set(Medicine::getUsageDesc, updated.getUsageDesc())
                .set(Medicine::getStatus, updated.getStatus());
        if (medicineDao.update(null, wrapper) == 0) {
            // An unchanged row can report 0 affected rows on MySQL; confirm it still exists.
            medicineDetail(id);
        }
        return medicineDetail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Medicine changeMedicineStatus(Long id, int status) {
        validateStatus(status);
        Medicine medicine = medicineDetail(id);
        if (medicine.getStatus() != null && medicine.getStatus() == status) return medicine;
        medicineDao.update(null, new LambdaUpdateWrapper<Medicine>()
                .eq(Medicine::getId, id).set(Medicine::getStatus, status));
        return medicineDetail(id);
    }

    public PageResult<AdminUserVO> users(String keyword, String role, Integer status, long current, int size) {
        validatePage(current, size);
        validateStatus(status);
        String normalizedRole = trimToNull(role);
        if (normalizedRole != null) {
            normalizedRole = normalizedRole.toUpperCase(java.util.Locale.ROOT);
            if (!List.of("ADMIN", "USER", "DOCTOR").contains(normalizedRole)) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "用户角色不合法");
            }
        }
        String normalizedKeyword = trimToNull(keyword);
        long offset = (current - 1) * size;
        return new PageResult<>(dataDao.selectUsers(normalizedKeyword, normalizedRole, status, offset, size),
                dataDao.countUsers(normalizedKeyword, normalizedRole, status), current, size);
    }

    public AdminUserVO userDetail(Long id) {
        AdminUserVO user = dataDao.selectUser(id);
        if (user == null) throw new BusinessException(BusinessErrorCode.NOT_FOUND, "用户不存在");
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO changeUserStatus(Long id, int status) {
        validateStatus(status);
        AdminUserVO user = userDetail(id);
        if (status == 0 && "ADMIN".equalsIgnoreCase(user.role())) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "管理员账号不允许禁用");
        }
        if (user.status() == status) return user;
        if (dataDao.updateUserStatus(id, status) == 0) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "用户不存在");
        }
        return userDetail(id);
    }

    public List<SysMenu> menus() {
        return dataDao.selectAdminMenus();
    }

    public PageResult<SysLoginLog> loginLogs(AdminLogPageQuery query) {
        DateWindow dates = dateWindow(query);
        long offset = (query.current() - 1) * query.size();
        String username = trimToNull(query.username());
        return new PageResult<>(dataDao.selectLoginLogs(username, query.status(), dates.start(), dates.endExclusive(), offset, query.size()),
                dataDao.countLoginLogs(username, query.status(), dates.start(), dates.endExclusive()), query.current(), query.size());
    }

    public PageResult<SysOperationLog> operationLogs(AdminLogPageQuery query) {
        DateWindow dates = dateWindow(query);
        long offset = (query.current() - 1) * query.size();
        String username = trimToNull(query.username());
        String module = trimToNull(query.module());
        return new PageResult<>(dataDao.selectOperationLogs(username, module, query.status(), dates.start(), dates.endExclusive(), offset, query.size()),
                dataDao.countOperationLogs(username, module, query.status(), dates.start(), dates.endExclusive()), query.current(), query.size());
    }

    private PrescriptionDetailVO requirePrescription(Long id) {
        PrescriptionDetailVO prescription = prescriptionDao.selectDetail(id);
        if (prescription == null) throw new BusinessException(BusinessErrorCode.NOT_FOUND, "处方不存在");
        return prescription;
    }

    private DateWindow dateWindow(AdminLogPageQuery query) {
        return new DateWindow(
                query.startDate() == null ? null : query.startDate().atStartOfDay(),
                query.endDate() == null ? null : query.endDate().plusDays(1).atStartOfDay());
    }

    private void validatePage(long current, int size) {
        if (current < 1 || size < 1 || size > 100) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "分页参数必须是 current>=1 且 size 为 1 到 100");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "status 只能是 0 或 1");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private record DateWindow(LocalDateTime start, LocalDateTime endExclusive) {
    }
}
