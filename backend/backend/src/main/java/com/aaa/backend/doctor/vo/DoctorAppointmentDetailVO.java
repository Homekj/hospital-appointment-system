package com.aaa.backend.doctor.vo;

import com.aaa.backend.appointment.vo.ConsultationVO;
import com.aaa.backend.contract.appointment.AppointmentSummary;
import lombok.Data;

/**
 * api.md 13.5 医生预约详情：响应同时包含预约、病历和处方摘要。
 * 病历 / 处方尚未创建时为 null（文档 3.4 约定可空字段返回 null）。
 */
@Data
public class DoctorAppointmentDetailVO {
    private AppointmentSummary appointment;
    private ConsultationVO consultation;
    private PrescriptionBriefVO prescription;
}
