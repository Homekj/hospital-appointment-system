package com.aaa.backend.service;

import com.aaa.backend.common.Result;
import com.aaa.backend.dto.ScheduleBatchDTO;
import com.aaa.backend.dto.ScheduleSaveDTO;
import com.aaa.backend.vo.ScheduleQuery;
import org.springframework.web.bind.annotation.RequestBody;

public interface ScheduleService {
    // 排班分页查询
    Result pageQuery(ScheduleQuery query);
    Result addSchedule(ScheduleSaveDTO scheduleSaveDTO);
    Result delete(Long id);
    Result changeStatus(Long id, Integer status);
    Result update(ScheduleSaveDTO dto);
    Result batchAdd( ScheduleBatchDTO dto);

    /** api.md 12.4 新增的 GET /api/admin/schedules/{id} 排班详情 */
    Result detail(Long id);
}