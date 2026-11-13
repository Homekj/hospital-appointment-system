package com.aaa.backend.doctor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * api.md 13.9 POST /api/doctor/appointments/{id}/complete 请求体。
 * prescriptionItems 必填但允许空数组：空数组表示「无处方完成」，直接把预约改为 completed。
 */
@Data
public class CompleteConsultationRequest {

    @NotNull(message = "prescriptionItems 不能为空，无处方请传空数组")
    @Valid
    private List<PrescriptionItemRequest> prescriptionItems;

    /** 旧 /complete-consultation 兼容入口用：构造一个「无处方完成」请求 */
    public static CompleteConsultationRequest empty() {
        CompleteConsultationRequest request = new CompleteConsultationRequest();
        request.setPrescriptionItems(new ArrayList<>());
        return request;
    }
}
