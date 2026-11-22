package com.aaa.backend.service.serviceimpl;

import com.aaa.backend.common.Result;
import com.aaa.backend.dao.DoctorDao;
import com.aaa.backend.dao.ScheduleDao;
import com.aaa.backend.dto.ScheduleBatchDTO;
import com.aaa.backend.dto.ScheduleSaveDTO;
import com.aaa.backend.entity.Doctor;
import com.aaa.backend.entity.Schedule;
import com.aaa.backend.service.ScheduleService;
import com.aaa.backend.vo.ScheduleQuery;
import com.aaa.backend.vo.ScheduleVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    /** 系统允许的固定出诊时段 */
    private static final Set<String> VALID_SLOTS = Set.of("08:00", "09:00", "10:00", "14:00", "15:00", "16:00");
    private static final int MAX_QUOTA = 1000;
    private static final BigDecimal MIN_FEE = new BigDecimal("0.01");
    private static final BigDecimal MAX_FEE = new BigDecimal("10000.00");
    private static final int MAX_BATCH_DAYS = 31;
    private static final int MAX_BATCH_ROWS = 1000;

    @Autowired
    private ScheduleDao scheduleDao;
    @Autowired
    private DoctorDao doctorDao;

    @Override
    public Result pageQuery(ScheduleQuery query) {
        // 与医生分页保持一致：null/非法值兜底 + pageSize 上限，避免一次查全表
        ScheduleQuery condition = query == null ? new ScheduleQuery() : query;
        int pageNum = condition.getPageNum() == null || condition.getPageNum() < 1
                ? 1 : condition.getPageNum();
        int pageSize = condition.getPageSize() == null || condition.getPageSize() < 1
                ? 10 : Math.min(condition.getPageSize(), 100);
        condition.setPageNum(pageNum);
        condition.setPageSize(pageSize);

        Page<ScheduleVO> page = new Page<>(pageNum, pageSize);
        IPage<ScheduleVO> result = scheduleDao.pageScheduleWithNames(page, condition);
        return Result.ok(result);
    }

    @Override
    public Result addSchedule(ScheduleSaveDTO dto) {
        if (dto == null || dto.getDoctorId() == null || dto.getScheduleDate() == null
                || dto.getTimeSlot() == null || dto.getTotalQuota() == null) {
            return Result.error("请填写完整的排班信息");
        }
        if (!VALID_SLOTS.contains(dto.getTimeSlot())) {
            return Result.error("非法时段！只能选择系统规定的整点时段。");
        }
        String feeError = validateFee(dto.getRegistrationFee());
        if (feeError != null) {
            return Result.error(feeError);
        }
        String quotaError = validateQuota(dto.getTotalQuota());
        if (quotaError != null) {
            return Result.error(quotaError);
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            return Result.error("状态只能是 0(停诊) 或 1(正常)");
        }
        if (dto.getScheduleDate().isBefore(LocalDate.now())) {
            return Result.error("不能为过去的日期新增排班");
        }
        String doctorError = validateDoctor(dto.getDoctorId());
        if (doctorError != null) {
            return Result.error(doctorError);
        }

        QueryWrapper<Schedule> wrapper = new QueryWrapper<>();
        wrapper.eq("doctor_id", dto.getDoctorId())
                .eq("schedule_date", dto.getScheduleDate())
                .eq("time_slot", dto.getTimeSlot());
        if (scheduleDao.selectCount(wrapper) > 0) {
            return Result.error("该医生在当天的该时段已有排班，请勿重复添加！");
        }

        Schedule schedule = new Schedule();
        BeanUtils.copyProperties(dto, schedule);
        schedule.setId(null);       // 强制自增，防止前端传假的 ID
        schedule.setBookedCount(0); // 新建排班已约人数一定是 0
        schedule.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        return scheduleDao.insert(schedule) > 0
                ? Result.ok("新增排班成功")
                : Result.error("新增排班失败");
    }

    @Override
    public Result delete(Long id) {
        if (id == null) {
            return Result.error("排班ID不能为空");
        }
        Schedule schedule = scheduleDao.selectById(id);
        if (schedule == null) {
            return Result.error("排班记录不存在");
        }
        // 只要有人预约了，坚决不让物理删除
        if (schedule.getBookedCount() != null && schedule.getBookedCount() > 0) {
            return Result.error("该排班已有患者预约，无法删除！请使用【停诊】功能。");
        }
        // 再按真实预约表兜底，防止 booked_count 与实际预约不一致时误删
        Long appointmentCount = scheduleDao.countActiveAppointment(id);
        if (appointmentCount != null && appointmentCount > 0) {
            return Result.error("该排班存在有效预约记录，无法删除！请使用【停诊】功能。");
        }
        return scheduleDao.deleteById(id) > 0
                ? Result.ok("删除成功")
                : Result.error("删除失败");
    }

    @Override
    public Result changeStatus(Long id, Integer status) {
        if (id == null) {
            return Result.error("排班ID不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("状态只能是 0(停诊) 或 1(正常)");
        }
        if (scheduleDao.selectById(id) == null) {
            return Result.error("排班记录不存在");
        }
        Schedule patch = new Schedule();
        patch.setId(id);
        patch.setStatus(status);
        if (scheduleDao.updateById(patch) <= 0) {
            return Result.error("状态更新失败");
        }
        return Result.ok(status == 0 ? "已停诊" : "已恢复");
    }

    @Override
    public Result update(ScheduleSaveDTO dto) {
        if (dto == null || dto.getId() == null) {
            return Result.error("排班ID不能为空");
        }
        String feeError = validateFee(dto.getRegistrationFee());
        if (feeError != null) {
            return Result.error(feeError);
        }
        Schedule existing = scheduleDao.selectById(dto.getId());
        if (existing == null) {
            return Result.error("排班记录不存在");
        }

        // 防超卖核心校验
        if (dto.getTotalQuota() != null) {
            String quotaError = validateQuota(dto.getTotalQuota());
            if (quotaError != null) {
                return Result.error(quotaError);
            }
            if (dto.getTotalQuota() < existing.getBookedCount()) {
                return Result.error("总号源不能小于已预约数(" + existing.getBookedCount() + ")");
            }
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            return Result.error("状态只能是 0(停诊) 或 1(正常)");
        }
        if (dto.getTimeSlot() != null && !VALID_SLOTS.contains(dto.getTimeSlot())) {
            return Result.error("非法时段！只能选择系统规定的整点时段。");
        }
        if (dto.getScheduleDate() != null && dto.getScheduleDate().isBefore(LocalDate.now())) {
            return Result.error("不能把排班改到过去的日期");
        }
        if (dto.getDoctorId() != null) {
            String doctorError = validateDoctor(dto.getDoctorId());
            if (doctorError != null) {
                return Result.error(doctorError);
            }
        }

        // 唯一键前置校验：按最终生效的「医生 + 日期 + 时段」判断，
        // 避免修改时段/日期/医生时撞 uk_schedule_doctor_date_slot 抛 500。
        Long finalDoctorId = dto.getDoctorId() != null ? dto.getDoctorId() : existing.getDoctorId();
        LocalDate finalDate = dto.getScheduleDate() != null ? dto.getScheduleDate() : existing.getScheduleDate();
        String finalSlot = dto.getTimeSlot() != null ? dto.getTimeSlot() : existing.getTimeSlot();
        Long conflict = scheduleDao.countConflict(finalDoctorId, finalDate, finalSlot, dto.getId());
        if (conflict != null && conflict > 0) {
            return Result.error("该医生在当天的该时段已有排班，请勿重复添加！");
        }

        Schedule schedule = new Schedule();
        BeanUtils.copyProperties(dto, schedule);
        // 绝对保护已预约数不被前端篡改（null 字段 MyBatis-Plus 会忽略）
        schedule.setBookedCount(null);
        return scheduleDao.updateById(schedule) > 0
                ? Result.ok("修改成功")
                : Result.error("修改失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result batchAdd(ScheduleBatchDTO dto) {
        // 1. 基础参数校验（totalQuota 必填，否则会撞 total_quota NOT NULL 变 500）
        if (dto == null || dto.getDoctorIds() == null || dto.getDoctorIds().isEmpty()
                || dto.getTimeSlots() == null || dto.getTimeSlots().isEmpty()
                || dto.getScheduleDateStart() == null || dto.getScheduleDateEnd() == null
                || dto.getTotalQuota() == null) {
            return Result.error("批量排班参数不完整");
        }
        String feeError = validateFee(dto.getRegistrationFee());
        if (feeError != null) {
            return Result.error(feeError);
        }
        String quotaError = validateQuota(dto.getTotalQuota());
        if (quotaError != null) {
            return Result.error(quotaError);
        }
        LocalDate start = dto.getScheduleDateStart();
        LocalDate end = dto.getScheduleDateEnd();
        if (start.isAfter(end)) {
            return Result.error("开始日期不能晚于结束日期");
        }
        if (start.isBefore(LocalDate.now())) {
            return Result.error("不能为过去的日期批量排班");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1L;
        if (days > MAX_BATCH_DAYS) {
            return Result.error("单次批量排班日期区间不能超过 " + MAX_BATCH_DAYS + " 天");
        }

        // 2. 列表去重 + 过滤空值，防止重复时段在同一事务内撞唯一键
        List<Long> doctorIds = new ArrayList<>(new LinkedHashSet<>(dto.getDoctorIds()));
        List<String> slots = new ArrayList<>(new LinkedHashSet<>(dto.getTimeSlots()));
        doctorIds.removeIf(Objects::isNull);
        slots.removeIf(slot -> slot == null || slot.isBlank());
        if (doctorIds.isEmpty() || slots.isEmpty()) {
            return Result.error("医生列表和时段列表不能为空");
        }

        // 3. 校验时段与医生
        for (String slot : slots) {
            if (!VALID_SLOTS.contains(slot)) {
                return Result.error("包含非法时段：" + slot);
            }
        }
        for (Long doctorId : doctorIds) {
            String doctorError = validateDoctor(doctorId);
            if (doctorError != null) {
                return Result.error(doctorError + "：" + doctorId);
            }
        }

        // 4. 总量上限，防止一次生成海量数据
        long planned = (long) doctorIds.size() * days * slots.size();
        if (planned > MAX_BATCH_ROWS) {
            return Result.error("单次最多生成 " + MAX_BATCH_ROWS + " 条排班，当前将生成 "
                    + planned + " 条，请缩小范围");
        }

        // 5. 算出日期区间内的每一天
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dateList.add(current);
            current = current.plusDays(1);
        }

        // 6. 查出区间内这些医生已有的排班，组装去重钥匙 doctorId_date_timeSlot
        QueryWrapper<Schedule> wrapper = new QueryWrapper<>();
        wrapper.in("doctor_id", doctorIds).between("schedule_date", start, end);
        List<Schedule> existingSchedules = scheduleDao.selectList(wrapper);
        Set<String> existingKeys = new HashSet<>();
        for (Schedule s : existingSchedules) {
            existingKeys.add(s.getDoctorId() + "_" + s.getScheduleDate() + "_" + s.getTimeSlot());
        }

        // 7. 三层循环生成排班（医生 × 日期 × 时段）
        List<Schedule> toInsertList = new ArrayList<>();
        Set<String> pendingKeys = new HashSet<>();
        for (Long doctorId : doctorIds) {
            for (LocalDate date : dateList) {
                for (String slot : slots) {
                    String key = doctorId + "_" + date + "_" + slot;
                    if (existingKeys.contains(key) || !pendingKeys.add(key)) {
                        continue;
                    }
                    Schedule schedule = new Schedule();
                    schedule.setDoctorId(doctorId);
                    schedule.setScheduleDate(date);
                    schedule.setTimeSlot(slot);
                    schedule.setRegistrationFee(dto.getRegistrationFee());
                    schedule.setTotalQuota(dto.getTotalQuota());
                    schedule.setBookedCount(0);
                    schedule.setStatus(1);
                    toInsertList.add(schedule);
                }
            }
        }

        if (toInsertList.isEmpty()) {
            return Result.ok("所选条件内已存在排班，无需重复生成。");
        }
        for (Schedule schedule : toInsertList) {
            scheduleDao.insert(schedule);
        }
        return Result.ok("批量排班成功！共生成 " + toInsertList.size() + " 条排班记录。");
    }

    /** api.md 12.4 排班详情；旧 /admin/schedule/** 没有该接口，这里补齐且不影响旧逻辑。 */
    @Override
    public Result detail(Long id) {
        if (id == null) {
            return Result.error("排班ID不能为空");
        }
        Schedule schedule = scheduleDao.selectById(id);
        if (schedule == null) {
            return Result.error("排班记录不存在");
        }
        return Result.ok(schedule);
    }

    private String validateFee(BigDecimal fee) {
        if (fee != null && (fee.compareTo(MIN_FEE) < 0 || fee.compareTo(MAX_FEE) > 0
                || fee.stripTrailingZeros().scale() > 2)) {
            return "挂号费必须在 0.01 至 10000.00 元之间，最多两位小数";
        }
        return null;
    }

    private String validateQuota(Integer quota) {
        if (quota == null) {
            return "总号源不能为空";
        }
        if (quota <= 0 || quota > MAX_QUOTA) {
            return "总号源必须大于 0 且不超过 " + MAX_QUOTA;
        }
        return null;
    }

    private String validateDoctor(Long doctorId) {
        Doctor doctor = doctorDao.selectById(doctorId);
        if (doctor == null) {
            return "医生不存在";
        }
        if (!Integer.valueOf(1).equals(doctor.getStatus())) {
            return "医生已停用，无法排班";
        }
        return null;
    }
}
