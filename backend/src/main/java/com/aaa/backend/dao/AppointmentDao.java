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

    /**
     * 条件更新状态：只有当前状态仍为 fromStatus 时才更新成功。
     * 这是防并发的关键：两个管理员同时点取消，只会有一个更新成功，
     * 从而保证 schedule.booked_count 只会被减一次。
     */
    int updateStatusIfMatch(@Param("id") Long id,
                            @Param("fromStatus") String fromStatus,
                            @Param("toStatus") String toStatus);

    /**
     * 取消预约时回滚号源。
     * 原子 UPDATE + booked_count > 0 条件，保证不会减成负数，也不需要先把值读出来再写回。
     */
    int decreaseBookedCount(@Param("scheduleId") Long scheduleId);

    /**
     * 患者端下单占用号源时使用，与回滚保持同一套原子写法。
     * booked_count &lt; total_quota 防止超卖。本模块暂不暴露接口，留给下单逻辑复用。
     */
    int increaseBookedCount(@Param("scheduleId") Long scheduleId);
}
