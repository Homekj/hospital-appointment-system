package com.aaa.backend.service;

import com.aaa.backend.common.Result;
import com.aaa.backend.dto.AppointmentStatusDTO;
import com.aaa.backend.vo.AppointmentQuery;
import com.aaa.backend.vo.AppointmentVO;

import java.util.List;

public interface AppointmentService {

    /** 查询条件校验，返回错误信息；null 表示通过。分页与导出共用，保证两者口径一致 */
    String validateQuery(AppointmentQuery query);

    Result page(AppointmentQuery query);

    Result detail(Long id);

    /** 取消：改 status=cancelled，并在同一事务里原子回滚号源 */
    Result cancel(Long id);

    /** 签到：仅 pending → checked_in */
    Result checkIn(Long id);

    /** 改状态：只放行 AppointmentStatus 定义的合法流转，且不允许直接改成 cancelled */
    Result changeStatus(Long id, AppointmentStatusDTO dto);

    /** 七个状态字典 + 每个状态的合法流转，供前端下拉与按钮渲染 */
    Result statusOptions();

    List<AppointmentVO> listForExport(AppointmentQuery query);
}
