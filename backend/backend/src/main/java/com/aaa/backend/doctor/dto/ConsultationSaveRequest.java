package com.aaa.backend.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存病历请求体（api.md 13.7）。
 *
 * <p>只允许提交病历内容；appointmentId 来自路径、doctorId 来自 JWT，
 * 请求体里即使带这两个字段也不会被读取。
 * 按文档字段约束：chiefComplaint / diagnosis 必填，其余可选。
 */
@Data
public class ConsultationSaveRequest {

    @NotBlank(message = "chiefComplaint 不能为空")
    @Size(max = 500, message = "chiefComplaint 不能超过 500 个字符")
    private String chiefComplaint;

    @Size(max = 1000, message = "presentIllness 不能超过 1000 个字符")
    private String presentIllness;

    @NotBlank(message = "diagnosis 不能为空")
    @Size(max = 500, message = "diagnosis 不能超过 500 个字符")
    private String diagnosis;

    @Size(max = 1000, message = "treatmentPlan 不能超过 1000 个字符")
    private String treatmentPlan;

    @Size(max = 500, message = "doctorAdvice 不能超过 500 个字符")
    private String doctorAdvice;
}
