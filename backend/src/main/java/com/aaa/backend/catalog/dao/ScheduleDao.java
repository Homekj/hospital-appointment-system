package com.aaa.backend.catalog.dao;

import com.aaa.backend.catalog.vo.ScheduleVO;
import com.aaa.backend.entity.Schedule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleDao extends BaseMapper<Schedule> {

    /** Lock the slot row before checking duplicate appointments and consuming quota. */
    @Select("SELECT * FROM schedule WHERE id = #{scheduleId} FOR UPDATE")
    Schedule selectByIdForUpdate(@Param("scheduleId") Long scheduleId);

    /**
     * 原子扣减号源：把「判断是否还有余量」和「+1」放进同一条 UPDATE。
     * MySQL 会对命中的行加排他锁，两个并发请求只有一个能拿到影响行数 1，
     * 其余拿到 0，从而杜绝超卖。绝不能改成「先 SELECT 余量再普通 UPDATE」。
     */
    @Update("""
            UPDATE schedule
               SET booked_count = booked_count + 1
             WHERE id = #{scheduleId}
               AND status = 1
               AND booked_count < total_quota
            """)
    int increaseBookedCount(@Param("scheduleId") Long scheduleId);

    /** 取消预约时归还号源，GREATEST 保证 booked_count 不会被减成负数。 */
    @Update("""
            UPDATE schedule
               SET booked_count = GREATEST(booked_count - 1, 0)
             WHERE id = #{scheduleId}
            """)
    int releaseBookedCount(@Param("scheduleId") Long scheduleId);

    /** 公开排班查询：只返回今天及以后、状态正常的排班，并算出剩余号源。 */
    @Select("""
            <script>
            SELECT s.id,
                   s.doctor_id,
                   s.schedule_date,
                   s.time_slot, s.registration_fee,
                   s.total_quota,
                   s.booked_count,
                   GREATEST(s.total_quota - s.booked_count, 0) AS remain_quota,
                   s.status
              FROM schedule s
             WHERE s.status = 1
               AND s.schedule_date >= CURDATE()
            <if test='doctorId != null'> AND s.doctor_id = #{doctorId} </if>
            <if test='scheduleDate != null'> AND s.schedule_date = #{scheduleDate} </if>
            <if test='startDate != null'> AND s.schedule_date >= #{startDate} </if>
            <if test='endDate != null'> AND #{endDate} >= s.schedule_date </if>
             ORDER BY s.schedule_date, s.time_slot, s.id
            </script>
            """)
    List<ScheduleVO> selectAvailableSchedules(@Param("doctorId") Long doctorId,
                                              @Param("scheduleDate") LocalDate scheduleDate,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
}
