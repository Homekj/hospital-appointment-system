package com.aaa.backend.doctor.service;

import com.aaa.backend.appointment.dao.ConsultationRecordDao;
import com.aaa.backend.appointment.vo.AppointmentDetailVO;
import com.aaa.backend.appointment.vo.ConsultationVO;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.contract.appointment.AppointmentCommandService;
import com.aaa.backend.contract.appointment.AppointmentPageQuery;
import com.aaa.backend.contract.appointment.AppointmentQueryService;
import com.aaa.backend.contract.appointment.AppointmentSummary;
import com.aaa.backend.doctor.dao.DoctorWorkbenchDao;
import com.aaa.backend.doctor.dao.MedicineDao;
import com.aaa.backend.doctor.dto.CompleteConsultationRequest;
import com.aaa.backend.doctor.dto.ConsultationSaveRequest;
import com.aaa.backend.doctor.dto.PrescriptionItemRequest;
import com.aaa.backend.doctor.vo.CompleteConsultationVO;
import com.aaa.backend.doctor.vo.DoctorAppointmentDetailVO;
import com.aaa.backend.doctor.vo.DoctorDashboardVO;
import com.aaa.backend.doctor.vo.DoctorProfileVO;
import com.aaa.backend.doctor.vo.DoctorQueueEntry;
import com.aaa.backend.doctor.vo.DoctorScheduleVO;
import com.aaa.backend.doctor.vo.DoctorStatsVO;
import com.aaa.backend.doctor.vo.MedicineOptionVO;
import com.aaa.backend.doctor.vo.PrescriptionBriefVO;
import com.aaa.backend.entity.ConsultationRecord;
import com.aaa.backend.entity.Medicine;
import com.aaa.backend.entity.Prescription;
import com.aaa.backend.entity.PrescriptionItem;
import com.aaa.backend.entity.enums.AppointmentStatus;
import com.aaa.backend.prescription.dao.PrescriptionDao;
import com.aaa.backend.prescription.dao.PrescriptionItemDao;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 医生工作台业务。
 *
 * <p><b>数据隔离约定（全模块统一）：</b>
 * 唯一可信的医生身份是 {@code CurrentUserContext.requireDoctorId()} 的返回值，
 * 它来自 JWT sub 对应的 sys_user.doctor_id，前端传的任何 doctorId 都不参与数据归属判断。
 * 所有列表查询强制带上该 ID；所有单条读写先做 {@link #requireOwnership}，
 * 记录属于别的医生时一律抛 403，而不是伪装成 404。
 *
 * <p>接口契约以 docs/api.md 第 13 节为准：stats / schedules / appointments /
 * start / clinical / complete / medicines。旧路径由 Controller 做别名兼容。
 */
@Service
public class DoctorWorkbenchService {

    private static final int MAX_SCHEDULE_DAYS = 366;

    private final DoctorWorkbenchDao workbenchDao;
    private final AppointmentQueryService queryService;
    private final AppointmentCommandService commandService;
    private final ConsultationRecordDao consultationRecordDao;
    private final PrescriptionDao prescriptionDao;
    private final PrescriptionItemDao prescriptionItemDao;
    private final MedicineDao medicineDao;

    public DoctorWorkbenchService(DoctorWorkbenchDao workbenchDao,
                                  AppointmentQueryService queryService,
                                  AppointmentCommandService commandService,
                                  ConsultationRecordDao consultationRecordDao,
                                  PrescriptionDao prescriptionDao,
                                  PrescriptionItemDao prescriptionItemDao,
                                  MedicineDao medicineDao) {
        this.workbenchDao = workbenchDao;
        this.queryService = queryService;
        this.commandService = commandService;
        this.consultationRecordDao = consultationRecordDao;
        this.prescriptionDao = prescriptionDao;
        this.prescriptionItemDao = prescriptionItemDao;
        this.medicineDao = medicineDao;
    }

    /** 前端显式传了别的 doctorId 时直接 403，不静默忽略请求参数 */
    public void assertSameDoctor(Long requestedDoctorId, Long currentDoctorId) {
        if (requestedDoctorId != null && !requestedDoctorId.equals(currentDoctorId)) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "无权访问其他医生的数据");
        }
    }

    public DoctorProfileVO profile(Long doctorId) {
        DoctorProfileVO vo = workbenchDao.selectProfile(doctorId);
        if (vo == null) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "医生信息不存在");
        }
        return vo;
    }

    public DoctorDashboardVO dashboard(Long doctorId) {
        LocalDate today = LocalDate.now();
        DoctorDashboardVO vo = workbenchDao.selectDashboard(doctorId, today);
        if (vo == null) {
            vo = new DoctorDashboardVO();
        }
        vo.setToday(today);
        return vo;
    }

    /** api.md 13.2：/stats 只返回文档规定的四个计数字段 */
    public DoctorStatsVO stats(Long doctorId) {
        DoctorDashboardVO dashboard = dashboard(doctorId);
        return new DoctorStatsVO(
                zeroIfNull(dashboard.getTodayAppointments()),
                zeroIfNull(dashboard.getWaitingCount()),
                zeroIfNull(dashboard.getInConsultationCount()),
                zeroIfNull(dashboard.getCompletedCount()));
    }

    private Long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }

    public List<DoctorScheduleVO> schedules(Long doctorId, LocalDate startDate,
                                            LocalDate endDate, Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "排班状态只能是 0(停诊) 或 1(正常)");
        }
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "开始日期不能晚于结束日期");
            }
            if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_SCHEDULE_DAYS) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST,
                        "查询日期区间不能超过 " + MAX_SCHEDULE_DAYS + " 天");
            }
        }
        List<DoctorScheduleVO> rows = workbenchDao.selectSchedules(doctorId, startDate, endDate, status);
        return rows == null ? List.of() : rows;
    }

    /** 只返回启用且有剩余可开数量的药品；处方状态隐式占用对应数量。 */
    public List<MedicineOptionVO> medicines(String keyword) {
        String value = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (value != null && value.length() > 100) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "keyword 不能超过 100 个字符");
        }
        List<MedicineOptionVO> rows = medicineDao.selectAvailable(value);
        return rows == null ? List.of() : rows;
    }

    public PageResult<AppointmentSummary> pageAppointments(Long doctorId, AppointmentPageQuery query) {
        AppointmentPageQuery condition = query == null
                ? new AppointmentPageQuery(1L, 10, null, null, null, null, null, null, null)
                : query;
        assertSameDoctor(condition.doctorId(), doctorId);
        // pageForDoctor 内部会用入参 doctorId 覆盖 query.doctorId，双保险
        return queryService.pageForDoctor(doctorId, condition);
    }

    /** api.md 13.5：预约 + 病历 + 处方摘要，且先做归属校验 */
    public DoctorAppointmentDetailVO appointmentDetail(Long appointmentId, Long doctorId) {
        requireOwnership(appointmentId, doctorId);
        DoctorAppointmentDetailVO vo = new DoctorAppointmentDetailVO();
        vo.setAppointment(queryService.getDetailForDoctor(appointmentId, doctorId));
        vo.setConsultation(consultationRecordDao.selectByAppointment(appointmentId));
        vo.setPrescription(workbenchDao.selectPrescriptionBrief(appointmentId));
        return vo;
    }

    public List<DoctorQueueEntry> todayQueue(Long doctorId) {
        return workbenchDao.selectTodayQueue(doctorId);
    }

    /** 开始接诊：checked_in -> in_consultation，契约实现本身幂等 */
    public void startConsultation(Long appointmentId, Long doctorId) {
        requireOwnership(appointmentId, doctorId);
        commandService.startConsultation(appointmentId, doctorId);
    }

    /**
     * api.md 13.9 完成接诊并开方。
     *
     * <p>关键约束：
     * <ul>
     *     <li>仅 in_consultation 可完成，且必须已有病历；</li>
     *     <li>价格只取服务端 medicine.price，不采信请求体里的任何金额；</li>
     *     <li>明细不能重复 medicineId，药品必须启用；</li>
     *     <li>无处方 → completed；有处方 → 创建 unpaid 处方 + 快照明细，预约 → pending_rx_pay；</li>
     *     <li>条件更新 + 处方写入在同一事务，重复请求走幂等分支，不会二次开方。</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public CompleteConsultationVO completeConsultation(Long appointmentId,
                                                       CompleteConsultationRequest request,
                                                       Long doctorId) {
        AppointmentDetailVO lockedAppointment = lockAppointmentForDoctor(appointmentId, doctorId);
        List<PrescriptionItemRequest> items = request == null || request.getPrescriptionItems() == null
                ? List.of() : request.getPrescriptionItems();

        AppointmentStatus status = AppointmentStatus.fromDatabaseValue(lockedAppointment.getStatus());
        if (status == AppointmentStatus.COMPLETED || status == AppointmentStatus.PENDING_RX_PAY) {
            // 幂等：把已产生的结果原样返回，不重置状态、不重复占用库存 / 不重复开方
            return existingResult(appointmentId, status);
        }
        if (!status.canCompleteConsultation()) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE,
                    "仅就诊中的预约可完成接诊");
        }
        if (consultationRecordDao.selectByAppointment(appointmentId) == null) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "请先填写病历再完成接诊");
        }
        Long existingCount = prescriptionDao.selectCount(new LambdaQueryWrapper<Prescription>()
                .eq(Prescription::getAppointmentId, appointmentId));
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "该预约已开处方，请勿重复提交");
        }

        List<PrescriptionItem> pendingItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (!items.isEmpty()) {
            Set<Long> seen = new HashSet<>();
            List<Long> medicineIds = new ArrayList<>();
            for (PrescriptionItemRequest item : items) {
                if (item == null || item.getMedicineId() == null) {
                    throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "medicineId 不能为空");
                }
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "quantity 必须大于 0");
                }
                if (item.getDays() != null && item.getDays() <= 0) {
                    throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "days 必须大于 0");
                }
                if (!seen.add(item.getMedicineId())) {
                    throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "处方明细不能包含重复药品");
                }
                medicineIds.add(item.getMedicineId());
            }

            Map<Long, Medicine> medicineMap = new HashMap<>();
            Map<Long, Integer> requestedQuantities = new HashMap<>();
            for (PrescriptionItemRequest item : items) {
                requestedQuantities.put(item.getMedicineId(), item.getQuantity());
            }
            // Lock in ID order so concurrent prescriptions for shared medicines serialize safely.
            medicineIds.sort(Long::compareTo);
            for (Long medicineId : medicineIds) {
                Medicine medicine = medicineDao.lockById(medicineId);
                if (medicine == null || !Integer.valueOf(1).equals(medicine.getStatus())) {
                    throw new BusinessException(BusinessErrorCode.BAD_REQUEST,
                            "药品不存在或已停用：" + medicineId);
                }
                int reserved = medicineDao.selectReservedQuantitiesForUpdate(medicineId).stream()
                        .filter(quantity -> quantity != null)
                        .mapToInt(Integer::intValue)
                        .sum();
                int available = Math.max(0, (medicine.getStock() == null ? 0 : medicine.getStock()) - reserved);
                if (requestedQuantities.get(medicineId) > available) {
                    throw new BusinessException(BusinessErrorCode.CONFLICT,
                            "药品可开库存不足，请刷新库存或调整数量：" + medicine.getName() + "（可用 " + available + "）");
                }
                medicineMap.put(medicine.getId(), medicine);
            }
            for (PrescriptionItemRequest item : items) {
                Medicine medicine = medicineMap.get(item.getMedicineId());
                if (medicine == null || !Integer.valueOf(1).equals(medicine.getStatus())) {
                    throw new BusinessException(BusinessErrorCode.BAD_REQUEST,
                            "药品不存在或已停用：" + item.getMedicineId());
                }
                BigDecimal unitPrice = medicine.getPrice() == null
                        ? BigDecimal.ZERO : medicine.getPrice();
                BigDecimal amount = unitPrice
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                        .setScale(2, RoundingMode.HALF_UP);

                PrescriptionItem entity = new PrescriptionItem();
                entity.setMedicineId(medicine.getId());
                entity.setMedicineName(medicine.getName());
                entity.setSpec(medicine.getSpec());
                entity.setDosage(trimToNull(item.getDosage()));
                entity.setFrequency(trimToNull(item.getFrequency()));
                entity.setDays(item.getDays());
                entity.setQuantity(item.getQuantity());
                entity.setUnitPrice(unitPrice);
                entity.setAmount(amount);
                pendingItems.add(entity);
                totalAmount = totalAmount.add(amount).setScale(2, RoundingMode.HALF_UP);
            }
        }

        boolean hasPrescription = !pendingItems.isEmpty();
        Long prescriptionId = null;
        BigDecimal prescriptionAmount = null;
        if (hasPrescription) {
            Prescription prescription = new Prescription();
            prescription.setAppointmentId(appointmentId);
            prescription.setUserId(workbenchDao.selectAppointmentUserId(appointmentId));
            prescription.setDoctorId(doctorId);
            prescription.setTotalAmount(totalAmount);
            prescription.setStatus("unpaid");
            // uk_prescription_appt 保证同一预约最多一张处方，并发时由唯一键兜底
            prescriptionDao.insert(prescription);
            for (PrescriptionItem item : pendingItems) {
                item.setPrescriptionId(prescription.getId());
                prescriptionItemDao.insert(item);
            }
            prescriptionId = prescription.getId();
            prescriptionAmount = totalAmount;
        }

        // Keep appointment state transitions in the shared appointment contract.
        // Prescription writes and the conditional state transition share this transaction,
        // so a failed transition also rolls back the prescription and its items.
        commandService.completeConsultation(appointmentId, doctorId);
        String finalStatus = AppointmentStatus.fromDatabaseValue(
                workbenchDao.selectAppointmentStatus(appointmentId)).value();
        return new CompleteConsultationVO(appointmentId, finalStatus, prescriptionId, prescriptionAmount);
    }

    private CompleteConsultationVO existingResult(Long appointmentId, AppointmentStatus status) {
        PrescriptionBriefVO brief = workbenchDao.selectPrescriptionBrief(appointmentId);
        return new CompleteConsultationVO(
                appointmentId,
                status.value(),
                brief == null ? null : brief.getId(),
                brief == null ? null : brief.getTotalAmount());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 开始接诊后的病历查看（api.md 13.8），未创建返回 404 */
    public ConsultationVO consultation(Long appointmentId, Long doctorId) {
        requireOwnership(appointmentId, doctorId);
        ConsultationVO vo = consultationRecordDao.selectByAppointment(appointmentId);
        if (vo == null) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "暂无病历");
        }
        return vo;
    }

    /**
     * 保存病历：只在「就诊中」允许，按 appointment_id 唯一键做插入/更新，重复保存幂等。
     * doctorId 一律取当前登录医生，不采信请求体。
     */
    @Transactional(rollbackFor = Exception.class)
    public ConsultationVO saveConsultation(Long appointmentId, ConsultationSaveRequest request, Long doctorId) {
        lockAppointmentForDoctor(appointmentId, doctorId);
        AppointmentStatus status = AppointmentStatus.fromDatabaseValue(
                workbenchDao.selectAppointmentStatus(appointmentId));
        if (!status.canSaveClinicalRecord()) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE,
                    "仅就诊中的预约可保存病历，请先开始接诊");
        }
        String chiefComplaint = checked(request == null ? null : request.getChiefComplaint(), "主诉", 500);
        String presentIllness = checked(request == null ? null : request.getPresentIllness(), "现病史", 1000);
        String diagnosis = checked(request == null ? null : request.getDiagnosis(), "诊断", 500);
        String treatmentPlan = checked(request == null ? null : request.getTreatmentPlan(), "治疗方案", 1000);
        String doctorAdvice = checked(request == null ? null : request.getDoctorAdvice(), "医嘱", 500);

        ConsultationRecord existing = consultationRecordDao.selectOne(
                new LambdaQueryWrapper<ConsultationRecord>()
                        .eq(ConsultationRecord::getAppointmentId, appointmentId));

        ConsultationRecord entity = existing == null ? new ConsultationRecord() : existing;
        entity.setAppointmentId(appointmentId);
        entity.setDoctorId(doctorId);
        entity.setChiefComplaint(chiefComplaint);
        entity.setPresentIllness(presentIllness);
        entity.setDiagnosis(diagnosis);
        entity.setTreatmentPlan(treatmentPlan);
        entity.setDoctorAdvice(doctorAdvice);

        if (existing == null) {
            consultationRecordDao.insert(entity);
        } else {
            UpdateWrapper<ConsultationRecord> update = new UpdateWrapper<>();
            update.eq("id", existing.getId())
                    .eq("appointment_id", appointmentId)
                    .eq("doctor_id", doctorId)
                    .set("chief_complaint", chiefComplaint)
                    .set("present_illness", presentIllness)
                    .set("diagnosis", diagnosis)
                    .set("treatment_plan", treatmentPlan)
                    .set("doctor_advice", doctorAdvice);
            consultationRecordDao.update(null, update);
        }
        return consultationRecordDao.selectByAppointment(appointmentId);
    }

    /**
     * 归属校验：先不带医生过滤查真实 owner，
     * 这样「不存在」返回 404、「属于别的医生」返回 403，两者不会被混为一谈。
     */
    private void requireOwnership(Long appointmentId, Long doctorId) {
        if (appointmentId == null) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "预约ID不能为空");
        }
        Long ownerId = workbenchDao.selectAppointmentOwner(appointmentId);
        if (ownerId == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND, "预约不存在");
        }
        if (!ownerId.equals(doctorId)) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "无权访问其他医生的数据");
        }
    }

    /** Lock first, then validate the JWT-bound doctor against the current schedule owner. */
    private AppointmentDetailVO lockAppointmentForDoctor(Long appointmentId, Long doctorId) {
        if (appointmentId == null) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "预约ID不能为空");
        }
        AppointmentDetailVO row = workbenchDao.lockAppointmentForDoctor(appointmentId);
        if (row == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND, "预约不存在");
        }
        if (!doctorId.equals(row.getDoctorId())) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "无权访问其他医生的数据");
        }
        return row;
    }

    private String checked(String value, String label, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST,
                    label + "不能超过 " + maxLength + " 个字符");
        }
        return trimmed.isEmpty() ? null : trimmed;
    }
}
