package com.aaa.backend.service.serviceimpl;

import com.aaa.backend.common.Result;
import com.aaa.backend.dao.AppointmentDao;
import com.aaa.backend.dto.AppointmentStatusDTO;
import com.aaa.backend.entity.Appointment;
import com.aaa.backend.entity.enums.AppointmentStatus;
import com.aaa.backend.service.AppointmentService;
import com.aaa.backend.vo.AppointmentQuery;
import com.aaa.backend.vo.AppointmentStatusOption;
import com.aaa.backend.vo.AppointmentVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_EXPORT_ROWS = 10000;

    /**
     * 七个状态严格按 sql/system.sql 中 appointment.status 的注释定义，
     * 不擅自新增前端出现过的其它写法；中文标签与后台下拉保持一致。
     */
    private static final Map<String, String> STATUS_LABELS = new LinkedHashMap<>();

    static {
        STATUS_LABELS.put(AppointmentStatus.UNPAID.value(), "待支付");
        STATUS_LABELS.put(AppointmentStatus.PENDING.value(), "待就诊");
        STATUS_LABELS.put(AppointmentStatus.CHECKED_IN.value(), "已到院");
        STATUS_LABELS.put(AppointmentStatus.IN_CONSULTATION.value(), "就诊中");
        STATUS_LABELS.put(AppointmentStatus.PENDING_RX_PAY.value(), "待缴药费");
        STATUS_LABELS.put(AppointmentStatus.COMPLETED.value(), "已完成");
        STATUS_LABELS.put(AppointmentStatus.CANCELLED.value(), "已取消");
    }

    @Autowired
    private AppointmentDao appointmentDao;

    @Override
    public String validateQuery(AppointmentQuery query) {
        if (query == null) {
            return null;
        }
        query.setStatus(trimToNull(query.getStatus()));
        query.setKeyword(trimToNull(query.getKeyword()));
        query.setScheduleDateStart(trimToNull(query.getScheduleDateStart()));
        query.setScheduleDateEnd(trimToNull(query.getScheduleDateEnd()));
        query.setScheduleDate(trimToNull(query.getScheduleDate()));

        if (query.getStatus() != null && !STATUS_LABELS.containsKey(query.getStatus())) {
            return "预约状态值不合法：" + query.getStatus() + "，合法值为 " + STATUS_LABELS.keySet();
        }
        if (query.getKeyword() != null && query.getKeyword().length() > 50) {
            return "搜索关键词不能超过 50 个字符";
        }
        if (query.getDoctorId() != null && query.getDoctorId() <= 0) {
            return "doctorId 必须为正数";
        }
        if (query.getDepartmentId() != null && query.getDepartmentId() <= 0) {
            return "departmentId 必须为正数";
        }
        String error = checkDate(query.getScheduleDateStart(), "开始日期");
        if (error != null) {
            return error;
        }
        error = checkDate(query.getScheduleDateEnd(), "结束日期");
        if (error != null) {
            return error;
        }
        error = checkDate(query.getScheduleDate(), "就诊日期");
        if (error != null) {
            return error;
        }
        if (query.getScheduleDateStart() != null && query.getScheduleDateEnd() != null
                && LocalDate.parse(query.getScheduleDateStart(), DateTimeFormatter.ISO_LOCAL_DATE)
                        .isAfter(LocalDate.parse(query.getScheduleDateEnd(), DateTimeFormatter.ISO_LOCAL_DATE))) {
            return "开始日期不能晚于结束日期";
        }
        return null;
    }

    @Override
    public Result page(AppointmentQuery query) {
        AppointmentQuery condition = query == null ? new AppointmentQuery() : query;
        String error = validateQuery(condition);
        if (error != null) {
            return Result.error(error);
        }
        int pageNum = condition.getPageNum() == null || condition.getPageNum() < 1
                ? 1 : condition.getPageNum();
        int pageSize = condition.getPageSize() == null || condition.getPageSize() < 1
                ? 10 : Math.min(condition.getPageSize(), MAX_PAGE_SIZE);
        condition.setPageNum(pageNum);
        condition.setPageSize(pageSize);

        Page<AppointmentVO> page = new Page<>(pageNum, pageSize);
        IPage<AppointmentVO> result = appointmentDao.pageAppointment(page, condition);
        enrich(result.getRecords());
        return Result.ok(result);
    }

    @Override
    public Result detail(Long id) {
        if (id == null) {
            return Result.error("预约ID不能为空");
        }
        AppointmentVO vo = appointmentDao.selectDetail(id);
        if (vo == null) {
            return Result.error("预约记录不存在");
        }
        enrichOne(vo);
        return Result.ok(vo);
    }

    /**
     * 取消预约：状态改为 cancelled，并在同一事务里原子回滚号源。
     *
     * <p>号源一致性三道保护：
     * <ol>
     *     <li>先按当前状态做条件 UPDATE，并发下只有一个请求能成功，不会重复减号源；</li>
     *     <li>回滚用 {@code booked_count = booked_count - 1 ... AND booked_count > 0}，
     *         不会减成负数，也不需要读出来再写回；</li>
     *     <li>两条写操作同处一个事务，任一条抛异常一起回滚。</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancel(Long id) {
        if (id == null) {
            return Result.error("预约ID不能为空");
        }
        Appointment appointment = appointmentDao.selectById(id);
        if (appointment == null) {
            return Result.error("预约记录不存在");
        }
        AppointmentStatus current = parseStatus(appointment.getStatus());
        if (current == null) {
            return Result.error("预约状态数据异常：" + appointment.getStatus());
        }
        if (current == AppointmentStatus.CANCELLED) {
            return Result.error("该预约已取消，请勿重复操作");
        }
        if (!current.canTransitionTo(AppointmentStatus.CANCELLED)) {
            return Result.error("当前状态【" + labelOf(current.value()) + "】不允许取消，允许的流转："
                    + transitionsOf(current.value()));
        }

        int updated = appointmentDao.updateStatusIfMatch(
                id, current.value(), AppointmentStatus.CANCELLED.value());
        if (updated == 0) {
            return Result.error("预约状态已变更，请刷新后重试");
        }

        int decreased = appointmentDao.decreaseBookedCount(appointment.getScheduleId());
        if (decreased == 0) {
            // 数据本来就不一致时宁可少减，也绝不把 booked_count 写成负数
            log.warn("取消预约 {} 时号源未回滚：schedule_id={} 不存在或 booked_count 已为 0",
                    id, appointment.getScheduleId());
        }
        return Result.ok("取消成功，号源已回滚");
    }

    /** 签到：严格限定 pending → checked_in */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result checkIn(Long id) {
        if (id == null) {
            return Result.error("预约ID不能为空");
        }
        Appointment appointment = appointmentDao.selectById(id);
        if (appointment == null) {
            return Result.error("预约记录不存在");
        }
        AppointmentStatus current = parseStatus(appointment.getStatus());
        if (current == null) {
            return Result.error("预约状态数据异常：" + appointment.getStatus());
        }
        if (!current.canCheckIn()) {
            return Result.error("只有【待就诊】的预约可以签到，当前状态：【"
                    + labelOf(current.value()) + "】");
        }
        int updated = appointmentDao.updateStatusIfMatch(
                id, current.value(), AppointmentStatus.CHECKED_IN.value());
        if (updated == 0) {
            return Result.error("预约状态已变更，请刷新后重试");
        }
        return Result.ok("签到成功");
    }

    /**
     * 改状态：只放行 AppointmentStatus.canTransitionTo 定义的合法流转。
     * 特别注意：禁止把状态直接改成 cancelled —— 取消必须走 cancel 接口，
     * 否则会绕过号源回滚，造成 schedule.booked_count 与真实预约数永久不一致。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result changeStatus(Long id, AppointmentStatusDTO dto) {
        if (id == null) {
            return Result.error("预约ID不能为空");
        }
        if (dto == null || dto.getStatus() == null || dto.getStatus().trim().isEmpty()) {
            return Result.error("目标状态不能为空");
        }
        String targetValue = dto.getStatus().trim();
        AppointmentStatus target = parseStatus(targetValue);
        if (target == null) {
            return Result.error("预约状态值不合法：" + targetValue + "，合法值为 " + STATUS_LABELS.keySet());
        }
        if (target == AppointmentStatus.CANCELLED) {
            return Result.error("取消预约需要回滚号源，请使用 PUT /{id}/cancel 接口，不能通过改状态直接取消");
        }
        Appointment appointment = appointmentDao.selectById(id);
        if (appointment == null) {
            return Result.error("预约记录不存在");
        }
        AppointmentStatus current = parseStatus(appointment.getStatus());
        if (current == null) {
            return Result.error("预约状态数据异常：" + appointment.getStatus());
        }
        if (current == target) {
            return Result.error("预约已处于【" + labelOf(target.value()) + "】状态");
        }
        if (!current.canTransitionTo(target)) {
            return Result.error("不允许的状态流转：" + labelOf(current.value()) + " → "
                    + labelOf(target.value()) + "；当前允许的流转：" + transitionsOf(current.value()));
        }
        int updated = appointmentDao.updateStatusIfMatch(id, current.value(), target.value());
        if (updated == 0) {
            return Result.error("预约状态已变更，请刷新后重试");
        }
        return Result.ok("状态已更新为【" + labelOf(target.value()) + "】");
    }

    @Override
    public Result statusOptions() {
        List<AppointmentStatusOption> options = new ArrayList<>();
        for (Map.Entry<String, String> entry : STATUS_LABELS.entrySet()) {
            options.add(new AppointmentStatusOption(
                    entry.getKey(), entry.getValue(), transitionsOf(entry.getKey())));
        }
        return Result.ok(options);
    }

    @Override
    public List<AppointmentVO> listForExport(AppointmentQuery query) {
        AppointmentQuery condition = query == null ? new AppointmentQuery() : query;
        String error = validateQuery(condition);
        if (error != null) {
            // 导出是文件下载，无法用 Result 包住错误体，交给全局异常处理返回 400
            throw new IllegalArgumentException(error);
        }
        List<AppointmentVO> rows = appointmentDao.listForExport(condition, MAX_EXPORT_ROWS);
        enrich(rows);
        return rows;
    }

    private void enrich(List<AppointmentVO> list) {
        if (list == null) {
            return;
        }
        for (AppointmentVO vo : list) {
            enrichOne(vo);
        }
    }

    private void enrichOne(AppointmentVO vo) {
        if (vo == null) {
            return;
        }
        vo.setStatusLabel(labelOf(vo.getStatus()));
        vo.setAllowedTransitions(transitionsOf(vo.getStatus()));
    }

    private String labelOf(String value) {
        return STATUS_LABELS.getOrDefault(value, value == null ? "" : value);
    }

    private List<String> transitionsOf(String value) {
        AppointmentStatus current = parseStatus(value);
        if (current == null) {
            return List.of();
        }
        return Arrays.stream(AppointmentStatus.values())
                .filter(current::canTransitionTo)
                .map(AppointmentStatus::value)
                .collect(Collectors.toList());
    }

    private AppointmentStatus parseStatus(String value) {
        if (value == null || !STATUS_LABELS.containsKey(value)) {
            return null;
        }
        try {
            return AppointmentStatus.fromDatabaseValue(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String checkDate(String value, String label) {
        if (value == null) {
            return null;
        }
        try {
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return null;
        } catch (DateTimeParseException exception) {
            return label + "格式不正确，应为 yyyy-MM-dd：" + value;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
