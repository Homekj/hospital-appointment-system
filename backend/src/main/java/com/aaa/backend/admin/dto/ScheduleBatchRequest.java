package com.aaa.backend.admin.dto;

import com.aaa.backend.dto.ScheduleBatchDTO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * api.md 12.4 批量排班请求体。
 *
 * <p>文档字段是 startDate / endDate，而旧模块 DTO 用的是
 * scheduleDateStart / scheduleDateEnd。这里做字段适配，旧 /admin/schedule/batch
 * 继续用原来的 DTO，互不影响。
 */
@Data
public class ScheduleBatchRequest {

    @NotEmpty(message = "doctorIds 不能为空")
    private List<Long> doctorIds;

    @NotNull(message = "startDate 不能为空")
    private LocalDate startDate;

    @NotNull(message = "endDate 不能为空")
    private LocalDate endDate;

    @NotEmpty(message = "timeSlots 不能为空")
    private List<String> timeSlots;

    @NotNull(message = "totalQuota 不能为空")
    @Min(value = 1, message = "totalQuota 必须大于 0")
    private Integer totalQuota;

    public ScheduleBatchDTO toDto() {
        ScheduleBatchDTO dto = new ScheduleBatchDTO();
        dto.setDoctorIds(doctorIds);
        dto.setScheduleDateStart(startDate);
        dto.setScheduleDateEnd(endDate);
        dto.setTimeSlots(timeSlots);
        dto.setTotalQuota(totalQuota);
        return dto;
    }
}
