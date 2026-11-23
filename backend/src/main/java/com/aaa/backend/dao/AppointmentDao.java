package com.aaa.backend.dao;

import com.aaa.backend.entity.Appointment;
import com.aaa.backend.vo.AppointmentQuery;
import com.aaa.backend.vo.AppointmentVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AppointmentDao extends BaseMapper<Appointment> {

    /** 四表联查分页 */
    IPage<AppointmentVO> pageAppointment(Page<?> page, @Param("query") AppointmentQuery query);

    /** 四表联查详情 */
    AppointmentVO selectDetail(@Param("id") Long id);

    /** 导出用（不分页，带行数上限） */
    List<AppointmentVO> listForExport(@Param("query") AppointmentQuery query, @Param("limit") int limit);

    /** 管理员到院签到，保留历史 confirmed 状态的兼容。 */
    int checkInOnScheduleDate(@Param("id") Long id);
}
