package com.aaa.backend.service.serviceimpl;

import com.aaa.backend.common.Result;
import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.catalog.dao.ScheduleDao;
import com.aaa.backend.contract.appointment.AppointmentCommandService;
import com.aaa.backend.dao.AppointmentDao;
import com.aaa.backend.dto.AppointmentStatusDTO;
import com.aaa.backend.entity.Appointment;
import com.aaa.backend.entity.Schedule;
import com.aaa.backend.entity.enums.AppointmentStatus;
import com.aaa.backend.service.AppointmentService;
import com.aaa.backend.vo.AppointmentQuery;
import com.aaa.backend.vo.AppointmentStatusOption;
import com.aaa.backend.vo.AppointmentVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppointmentServiceImpl implements AppointmentService {

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
    @Autowired
    private ScheduleDao scheduleDao;
    @Autowired
    private AppointmentCommandService commandService;
    @Autowired
    private CurrentUserContext currentUser;

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

    @Override
    public Result cancel(Long id) {
        if (id == null) {
            return Result.error("预约ID不能为空");
        }
        commandService.cancelByAdmin(id, currentUser.requireUserId());
        return Result.ok("取消成功，号源已释放");
    }

    /** 签到：严格限定 pending → checked_in */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Result checkIn(Long id) {
        if (id == null) {
            return Result.error("预约ID不能为空");
        }
        Schedule schedule = scheduleDao.selectByAppointmentForUpdate(id, null);
        Appointment appointment = appointmentDao.selectById(id);
        if (appointment == null) {
            return Result.error("预约记录不存在");
        }
        if (schedule == null || !Integer.valueOf(1).equals(schedule.getStatus())) {
            return Result.error("该排班已停诊");
        }
        AppointmentStatus current = parseStatus(appointment.getStatus());
        if (current == null) {
            return Result.error("预约状态数据异常：" + appointment.getStatus());
        }
        if (!current.canCheckIn()) {
            return Result.error("只有【待就诊】的预约可以签到，当前状态：【"
                    + labelOf(current.value()) + "】");
        }
        if (appointmentDao.checkInOnScheduleDate(id) != 1) {
            return Result.error("仅可在就诊当天签到，请核对预约日期和状态");
        }
        return Result.ok("签到成功");
    }

    /** 兼容旧客户端的签到操作，支付和诊疗均使用各自的业务接口。 */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Result changeStatus(Long id, AppointmentStatusDTO dto) {
        if (dto == null || dto.getStatus() == null || dto.getStatus().isBlank()) {
            return Result.error("目标状态不能为空");
        }
        if (!AppointmentStatus.CHECKED_IN.value().equals(dto.getStatus().trim())) {
            return Result.error("本入口仅支持签到，请通过支付、取消、医生接诊或药房发药流程更新预约");
        }
        return checkIn(id);
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
        AppointmentStatus status = parseStatus(value);
        return status == null ? (value == null ? "" : value) : STATUS_LABELS.get(status.value());
    }

    private List<String> transitionsOf(String value) {
        AppointmentStatus current = parseStatus(value);
        if (current == null || !current.canCancelByAdmin()) {
            return List.of();
        }
        return current.canCheckIn() ? List.of("checked_in", "cancelled") : List.of("cancelled");
    }

    private AppointmentStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
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
