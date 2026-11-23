package com.aaa.backend.doctor.controller;

import com.aaa.backend.appointment.vo.ConsultationVO;
import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.profile.dto.AvatarUpdateRequest;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.contract.appointment.AppointmentPageQuery;
import com.aaa.backend.contract.appointment.AppointmentSummary;
import com.aaa.backend.doctor.dto.CompleteConsultationRequest;
import com.aaa.backend.doctor.dto.ConsultationSaveRequest;
import com.aaa.backend.doctor.dto.DoctorPasswordUpdateRequest;
import com.aaa.backend.doctor.service.DoctorAccountService;
import com.aaa.backend.doctor.service.DoctorWorkbenchService;
import com.aaa.backend.doctor.vo.CompleteConsultationVO;
import com.aaa.backend.doctor.vo.DoctorAppointmentDetailVO;
import com.aaa.backend.doctor.vo.DoctorDashboardVO;
import com.aaa.backend.doctor.vo.DoctorProfileVO;
import com.aaa.backend.doctor.vo.DoctorQueueEntry;
import com.aaa.backend.doctor.vo.DoctorScheduleVO;
import com.aaa.backend.doctor.vo.DoctorStatsVO;
import com.aaa.backend.doctor.vo.MedicineOptionVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 医生工作台接口（api.md 第 13 节，/api/doctor 域）。
 *
 * <p>鉴权链：JwtInterceptor 校验 Bearer token → RoleAccessPolicy 限定 /api/doctor 仅 DOCTOR
 * → CurrentUserContext.requireDoctorId() 取 JWT 对应账号绑定的 doctor_id。
 * 任何接口都不接受「我要看哪个医生」作为数据主体；显式传他人 doctorId 一律 403，
 * 访问其他医生的预约详情 / 病历 / 接诊动作同样一律 403。
 *
 * <p>返回体统一为 {@link ApiResponse}（{code, data, message}），与小程序 utils/request.js 一致。
 * 旧路径以别名方式保留，避免已经联调过的前端突然 404。
 */
@RestController
@RequestMapping("/api/doctor")
public class DoctorWorkbenchController {

    private final DoctorWorkbenchService service;
    private final DoctorAccountService accountService;
    private final CurrentUserContext currentUserContext;

    public DoctorWorkbenchController(DoctorWorkbenchService service,
                                     DoctorAccountService accountService,
                                     CurrentUserContext currentUserContext) {
        this.service = service;
        this.accountService = accountService;
        this.currentUserContext = currentUserContext;
    }

    /** api.md 13.1 当前医生信息（不接受 id 参数，只返回 JWT 绑定的医生） */
    @GetMapping("/me")
    public ApiResponse<DoctorProfileVO> me() {
        return ApiResponse.success(service.profile(currentUserContext.requireDoctorId()));
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@Valid @RequestBody DoctorPasswordUpdateRequest request) {
        accountService.updatePassword(currentUserContext.requireUserId(), request);
        return ApiResponse.success();
    }

    @PutMapping("/avatar")
    public ApiResponse<LoginUser> updateAvatar(@Valid @RequestBody AvatarUpdateRequest request) {
        return ApiResponse.success(accountService.updateAvatar(currentUserContext.requireUserId(),
                currentUserContext.requireDoctorId(), request.path()));
    }

    /** api.md 13.2 工作台统计：只返回文档规定的四个计数 */
    @GetMapping("/stats")
    public ApiResponse<DoctorStatsVO> stats() {
        return ApiResponse.success(service.stats(currentUserContext.requireDoctorId()));
    }

    /** 旧路径别名：字段更全的统计对象，保留给已联调页面 */
    @GetMapping("/dashboard")
    public ApiResponse<DoctorDashboardVO> dashboard() {
        return ApiResponse.success(service.dashboard(currentUserContext.requireDoctorId()));
    }

    /** api.md 13.3 本人排班：可按日期区间与状态筛选，最多返回 200 条 */
    @GetMapping("/schedules")
    public ApiResponse<List<DoctorScheduleVO>> schedules(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long doctorId) {
        Long currentDoctorId = currentUserContext.requireDoctorId();
        service.assertSameDoctor(doctorId, currentDoctorId);
        return ApiResponse.success(service.schedules(currentDoctorId, startDate, endDate, status));
    }

    /**
     * api.md 13.4 本人预约列表：current / size / status / scheduleDate 分页。
     * 同时兼容旧前端的 pageNum / pageSize / keyword / startDate / endDate 写法。
     * 日期区间固定按就诊日期 schedule.schedule_date 过滤。
     */
    @GetMapping("/appointments")
    public ApiResponse<PageResult<AppointmentSummary>> appointments(
            @RequestParam(required = false) Long pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate) {
        Long currentDoctorId = currentUserContext.requireDoctorId();
        Long requestedPage = current != null ? current : pageNum;
        Integer requestedSize = size != null ? size : pageSize;
        long page = requestedPage == null || requestedPage < 1 ? 1L : requestedPage;
        int limit = requestedSize == null || requestedSize < 1 ? 10 : Math.min(requestedSize, 100);
        String nameFilter = patientName != null && !patientName.isBlank() ? patientName.trim() : keyword;
        AppointmentPageQuery query = new AppointmentPageQuery(
                page, limit, blankToNull(status), doctorId, departmentId,
                blankToNull(nameFilter), startDate, endDate, scheduleDate);
        return ApiResponse.success(service.pageAppointments(currentDoctorId, query));
    }

    @GetMapping("/queue")
    public ApiResponse<List<DoctorQueueEntry>> queue() {
        return ApiResponse.success(service.todayQueue(currentUserContext.requireDoctorId()));
    }

    /** api.md 13.5 本人预约详情：预约 + 病历 + 处方摘要；属于其他医生返回 403 */
    @GetMapping("/appointments/{id}")
    public ApiResponse<DoctorAppointmentDetailVO> appointmentDetail(@PathVariable Long id) {
        return ApiResponse.success(service.appointmentDetail(id, currentUserContext.requireDoctorId()));
    }

    /** api.md 13.8 查看病历；未创建返回 404。/consultation 为旧路径别名 */
    @GetMapping({"/appointments/{id}/clinical", "/appointments/{id}/consultation"})
    public ApiResponse<ConsultationVO> consultation(@PathVariable Long id) {
        return ApiResponse.success(service.consultation(id, currentUserContext.requireDoctorId()));
    }

    @PostMapping("/appointments/{id}/queue/skip")
    public ApiResponse<Void> skipQueue(@PathVariable Long id) {
        service.setQueueDeferred(id, currentUserContext.requireDoctorId(), true);
        return ApiResponse.success();
    }

    @PostMapping("/appointments/{id}/queue/rejoin")
    public ApiResponse<Void> rejoinQueue(@PathVariable Long id) {
        service.setQueueDeferred(id, currentUserContext.requireDoctorId(), false);
        return ApiResponse.success();
    }

    /** api.md 13.6 开始接诊：checked_in → in_consultation，重复调用幂等 */
    @PostMapping("/appointments/{id}/start")
    public ApiResponse<Void> start(@PathVariable Long id) {
        service.startConsultation(id, currentUserContext.requireDoctorId());
        return ApiResponse.success();
    }

    /** 旧路径别名：PUT /appointments/{id}/start-consultation */
    @PutMapping("/appointments/{id}/start-consultation")
    public ApiResponse<Void> startConsultationLegacy(@PathVariable Long id) {
        return start(id);
    }

    /** api.md 13.7 保存病历：仅「就诊中」可写，按 appointment_id 唯一键 upsert */
    @PutMapping({"/appointments/{id}/clinical", "/appointments/{id}/consultation"})
    public ApiResponse<ConsultationVO> saveConsultation(@PathVariable Long id,
                                                         @Valid @RequestBody ConsultationSaveRequest request) {
        return ApiResponse.success(service.saveConsultation(
                id, request, currentUserContext.requireDoctorId()));
    }

    /**
     * api.md 13.9 完成接诊并开方。
     * Idempotency-Key 是可选请求头（文档「可使用」）；即使不传，
     * 服务端也通过 in_consultation 条件更新 + uk_prescription_appt 保证幂等。
     */
    @PostMapping("/appointments/{id}/complete")
    public ApiResponse<CompleteConsultationVO> complete(
            @PathVariable Long id,
            @Valid @RequestBody CompleteConsultationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.success(service.completeConsultation(
                id, request, currentUserContext.requireDoctorId()));
    }

    /** 旧路径别名：无请求体，等价于「无处方完成」 */
    @PutMapping("/appointments/{id}/complete-consultation")
    public ApiResponse<CompleteConsultationVO> completeConsultationLegacy(@PathVariable Long id) {
        return ApiResponse.success(service.completeConsultation(
                id, CompleteConsultationRequest.empty(), currentUserContext.requireDoctorId()));
    }

    /** api.md 13.10 查询可用药品：只返回启用且有库存的药品 */
    @GetMapping("/medicines")
    public ApiResponse<List<MedicineOptionVO>> medicines(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(service.medicines(keyword));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
