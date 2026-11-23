package com.aaa.backend.dao;

import com.aaa.backend.entity.Schedule;
import com.aaa.backend.vo.ScheduleVO;
import com.aaa.backend.vo.ScheduleQuery;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

public interface ScheduleDao extends BaseMapper<Schedule> {
    IPage<ScheduleVO> pageScheduleWithNames(Page<?> page, @Param("query") ScheduleQuery query);

    @Select("SELECT * FROM schedule WHERE id = #{id} FOR UPDATE")
    Schedule lockById(@Param("id") Long id);

    /** 已取消预约仍需引用排班，以展示原就诊安排。 */
    @Select("SELECT COUNT(*) FROM appointment WHERE schedule_id = #{scheduleId}")
    long countAppointments(@Param("scheduleId") Long scheduleId);

    /**
     * 修改排班时检查唯一键 (doctor_id, schedule_date, time_slot) 是否已被其他记录占用，
     * 避免直接抛 DuplicateKeyException 变成 500。
     */
    @Select("SELECT COUNT(*) FROM schedule WHERE doctor_id = #{doctorId} " +
            "AND schedule_date = #{scheduleDate} AND time_slot = #{timeSlot} AND id <> #{excludeId}")
    Long countConflict(@Param("doctorId") Long doctorId,
                       @Param("scheduleDate") LocalDate scheduleDate,
                       @Param("timeSlot") String timeSlot,
                       @Param("excludeId") Long excludeId);
}
