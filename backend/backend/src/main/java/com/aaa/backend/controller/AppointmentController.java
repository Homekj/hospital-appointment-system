package com.aaa.backend.controller;

import com.aaa.backend.admin.support.ResultBridge;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.dto.AppointmentStatusDTO;
import com.aaa.backend.service.AppointmentService;
import com.aaa.backend.vo.AppointmentQuery;
import com.aaa.backend.vo.AppointmentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 预约管理（后台 ADMIN 专用）。
 *
 * <p>主路径按需求固定为 /api/admin/appointments，同时兼容本项目其它管理模块
 * 使用的 /admin/appointment 前缀，方便现有前端直接联调。
 *
 * <p>只开放三个状态动作，不提供新增/修改/删除预约，也不提供修改 amount 的入口：
 * <ul>
 *     <li>PUT {id}/cancel    取消（含号源回滚）</li>
 *     <li>PUT {id}/check-in  签到（pending → checked_in）</li>
 *     <li>PUT {id}/status    合法流转内改状态</li>
 * </ul>
 */
/**
 * 显式指定 bean 名：远程患者端也有一个 AppointmentController（/api/appointments），
 * 两者简单类名相同，默认 bean 名会冲突导致整个应用启动失败。
 */
@RestController("adminAppointmentController")
@RequestMapping({"/api/admin/appointments", "/admin/appointment"})
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    /**
     * 分页列表（api.md 12.5）。
     *
     * <p>文档参数 current / size / patientName / startDate / endDate 在这里适配为
     * 旧模块的 pageNum / pageSize / keyword / scheduleDateStart / scheduleDateEnd；
     * 旧参数仍可直接使用，两套前端都不会被破坏。
     */
    @GetMapping(value = {"", "/page"})
    public ApiResponse<PageResult<Object>> page(
            AppointmentQuery query,
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        AppointmentQuery condition = query == null ? new AppointmentQuery() : query;
        if (current != null) {
            condition.setPageNum(current);
        }
        if (size != null) {
            condition.setPageSize(size);
        }
        if (notBlank(patientName) && condition.getKeyword() == null) {
            condition.setKeyword(patientName.trim());
        }
        if (notBlank(startDate) && condition.getScheduleDateStart() == null) {
            condition.setScheduleDateStart(startDate.trim());
        }
        if (notBlank(endDate) && condition.getScheduleDateEnd() == null) {
            condition.setScheduleDateEnd(endDate.trim());
        }
        return ResultBridge.page(appointmentService.page(condition));
    }

    /** 详情（与医生、排班模块一致的 ?id= 写法） */
    @GetMapping("/detail")
    public ApiResponse<Object> detail(@RequestParam Long id) {
        return ResultBridge.data(appointmentService.detail(id));
    }

    /** 详情（RESTful 写法，正则限定纯数字，避免与 /export 等字面量路径冲突） */
    @GetMapping("/{id:\\d+}")
    public ApiResponse<Object> detailById(@PathVariable Long id) {
        return ResultBridge.data(appointmentService.detail(id));
    }

    /** 七个状态字典及各自允许的流转，供前端下拉与操作按钮渲染 */
    @GetMapping("/status-options")
    public ApiResponse<Object> statusOptions() {
        return ResultBridge.data(appointmentService.statusOptions());
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Object> cancel(@PathVariable Long id) {
        return ResultBridge.data(appointmentService.cancel(id));
    }

    @PutMapping("/{id}/check-in")
    public ApiResponse<Object> checkIn(@PathVariable Long id) {
        return ResultBridge.data(appointmentService.checkIn(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Object> changeStatus(@PathVariable Long id, @RequestBody AppointmentStatusDTO dto) {
        return ResultBridge.data(appointmentService.changeStatus(id, dto));
    }

    /**
     * CSV 导出（可选能力）。
     *
     * <p>敏感信息处理策略，刻意固定不提供「关闭脱敏」的开关：
     * <ul>
     *     <li>身份证号：整列不导出，后台列表/详情也不返回；</li>
     *     <li>手机号：保留前 3 后 4，中间打码，例如 157****8599；</li>
     *     <li>所有单元格做 CSV 转义，并对 = + - @ 开头的字段加前缀防公式注入。</li>
     * </ul>
     * 导出上限 10000 行，防止一次拉全表。
     */
    @GetMapping("/export")
    public ResponseEntity<?> export(AppointmentQuery query) {
        AppointmentQuery condition = query == null ? new AppointmentQuery() : query;
        String error = appointmentService.validateQuery(condition);
        if (error != null) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(400, error));
        }
        List<AppointmentVO> rows = appointmentService.listForExport(condition);
        byte[] body = buildCsv(rows).getBytes(StandardCharsets.UTF_8);
        String filename = "appointments_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .contentLength(body.length)
                .body(body);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String buildCsv(List<AppointmentVO> rows) {
        StringBuilder csv = new StringBuilder();
        // BOM，避免 Excel 直接打开时中文乱码
        csv.append('\uFEFF');
        csv.append("预约ID,就诊人,手机号(脱敏),科室,医生,就诊日期,时段,状态,挂号费,支付方式,支付时间,下单时间,备注\r\n");
        if (rows != null) {
            for (AppointmentVO vo : rows) {
                csv.append(cell(vo.getId())).append(',')
                        .append(cell(vo.getPatientName())).append(',')
                        .append(cell(maskPhone(vo.getPatientPhone()))).append(',')
                        .append(cell(vo.getDepartmentName())).append(',')
                        .append(cell(vo.getDoctorName())).append(',')
                        .append(cell(vo.getScheduleDate())).append(',')
                        .append(cell(vo.getTimeSlot())).append(',')
                        .append(cell(vo.getStatusLabel())).append(',')
                        .append(cell(vo.getAmount())).append(',')
                        .append(cell(vo.getPayMethod())).append(',')
                        .append(cell(vo.getPayTime())).append(',')
                        .append(cell(vo.getCreateTime())).append(',')
                        .append(cell(vo.getRemark())).append("\r\n");
            }
        }
        return csv.toString();
    }

    private String cell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        // 防 CSV 公式注入：以 = + - @ 开头的字段前置单引号
        if (text.startsWith("=") || text.startsWith("+")
                || text.startsWith("-") || text.startsWith("@")) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String value = phone.trim();
        if (value.length() <= 4) {
            return "****";
        }
        if (value.length() < 7) {
            return value.charAt(0) + "****";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
