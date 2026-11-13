package com.aaa.backend.appointment.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** 患者查看的病历对象，对应 docs/api.md 8.7。 */
@Data
public class ConsultationVO {
    private Long id;
    private Long appointmentId;
    private Long doctorId;
    private String doctorName;
    private String chiefComplaint;
    private String presentIllness;
    private String diagnosis;
    private String treatmentPlan;
    private String doctorAdvice;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
