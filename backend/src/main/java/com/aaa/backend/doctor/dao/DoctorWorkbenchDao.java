package com.aaa.backend.doctor.dao;

import com.aaa.backend.appointment.vo.AppointmentDetailVO;
import com.aaa.backend.doctor.vo.DoctorDashboardVO;
import com.aaa.backend.doctor.vo.DoctorProfileVO;
import com.aaa.backend.doctor.vo.DoctorQueueEntry;
import com.aaa.backend.doctor.vo.DoctorScheduleVO;
import com.aaa.backend.doctor.vo.PrescriptionBriefVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 医生工作台查询。所有 SQL 的 doctor_id 都由服务端从 JWT 注入，
 * 不接受前端作为查询主体传入；唯一例外是「归属校验」查询，它故意不过滤医生，
 * 用来区分「记录不存在 404」与「属于别的医生 403」。
 */
public interface DoctorWorkbenchDao {

    @Select("""
            SELECT a.id, a.patient_name, s.time_slot, a.status, a.check_in_time
              FROM schedule s JOIN appointment a ON a.schedule_id = s.id
             WHERE s.doctor_id = #{doctorId} AND s.schedule_date = CURDATE()
               AND a.status IN ('checked_in', 'in_consultation')
             ORDER BY (a.status = 'in_consultation') DESC, s.time_slot, s.id,
                      COALESCE(a.check_in_time, a.update_time), a.id
            """)
    List<DoctorQueueEntry> selectTodayQueue(@Param("doctorId") Long doctorId);

    /** Current locking read for doctor-owned appointment writes. */
    @Select("""
            SELECT a.id, a.user_id AS userId, a.schedule_id AS scheduleId,
                   s.doctor_id AS doctorId, a.status
              FROM appointment a
              JOIN schedule s ON s.id = a.schedule_id
             WHERE a.id = #{appointmentId}
             FOR UPDATE
            """)
    AppointmentDetailVO lockAppointmentForDoctor(@Param("appointmentId") Long appointmentId);

    @Select("""
            SELECT d.id, d.name, d.title, d.gender, d.avatar, d.intro, d.status,
                   d.department_id AS departmentId,
                   dep.name        AS departmentName
              FROM doctor d
              LEFT JOIN department dep ON dep.id = d.department_id
             WHERE d.id = #{doctorId}
            """)
    DoctorProfileVO selectProfile(@Param("doctorId") Long doctorId);

    /**
     * 工作台统计：用一组标量子查询一次性取回，每个计数都是 COUNT(*)，
     * 类型稳定为 BIGINT，避免 SUM 返回 DECIMAL 造成的类型转换问题。
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM appointment a
                 JOIN schedule s ON s.id = a.schedule_id
                WHERE s.doctor_id = #{doctorId} AND a.status <> 'cancelled')          AS totalAppointments,
              (SELECT COUNT(*) FROM appointment a
                 JOIN schedule s ON s.id = a.schedule_id
                WHERE s.doctor_id = #{doctorId} AND s.schedule_date = #{today}
                  AND a.status <> 'cancelled')                                        AS todayAppointments,
              (SELECT COUNT(*) FROM appointment a
                 JOIN schedule s ON s.id = a.schedule_id
                WHERE s.doctor_id = #{doctorId}
                  AND a.status IN ('pending', 'checked_in'))                          AS waitingCount,
              (SELECT COUNT(*) FROM appointment a
                 JOIN schedule s ON s.id = a.schedule_id
                WHERE s.doctor_id = #{doctorId} AND a.status = 'in_consultation')     AS inConsultationCount,
              (SELECT COUNT(*) FROM appointment a
                 JOIN schedule s ON s.id = a.schedule_id
                WHERE s.doctor_id = #{doctorId} AND a.status = 'pending_rx_pay')      AS pendingRxPayCount,
              (SELECT COUNT(*) FROM appointment a
                 JOIN schedule s ON s.id = a.schedule_id
                WHERE s.doctor_id = #{doctorId} AND a.status = 'completed')           AS completedCount,
              (SELECT COUNT(*) FROM schedule s
                WHERE s.doctor_id = #{doctorId} AND s.schedule_date = #{today})       AS todaySchedules,
              (SELECT COALESCE(SUM(GREATEST(s.total_quota - s.booked_count, 0)), 0)
                 FROM schedule s
                WHERE s.doctor_id = #{doctorId} AND s.schedule_date = #{today}
                  AND s.status = 1)                                                   AS todayRemainQuota
            """)
    DoctorDashboardVO selectDashboard(@Param("doctorId") Long doctorId,
                                      @Param("today") LocalDate today);

    @Select("""
            <script>
            SELECT s.id,
                   s.schedule_date AS scheduleDate,
                   s.time_slot     AS timeSlot,
                   s.total_quota   AS totalQuota,
                   s.booked_count  AS bookedCount,
                   GREATEST(s.total_quota - s.booked_count, 0) AS remainQuota,
                   s.status,
                   (SELECT COUNT(*) FROM appointment a
                     WHERE a.schedule_id = s.id AND a.status != 'cancelled') AS appointmentCount
              FROM schedule s
             WHERE s.doctor_id = #{doctorId}
            <if test='startDate != null'> AND s.schedule_date &gt;= #{startDate} </if>
            <if test='endDate != null'>   AND s.schedule_date &lt;= #{endDate} </if>
            <if test='status != null'>    AND s.status = #{status} </if>
             ORDER BY s.schedule_date DESC, s.time_slot DESC, s.id DESC
             LIMIT 200
            </script>
            """)
    List<DoctorScheduleVO> selectSchedules(@Param("doctorId") Long doctorId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("status") Integer status);

    /** 归属校验：只按预约 ID 查真实所属医生，不按调用者过滤 */
    @Select("""
            SELECT s.doctor_id
              FROM appointment a
              JOIN schedule s ON s.id = a.schedule_id
             WHERE a.id = #{appointmentId}
            """)
    Long selectAppointmentOwner(@Param("appointmentId") Long appointmentId);

    @Select("SELECT status FROM appointment WHERE id = #{appointmentId}")
    String selectAppointmentStatus(@Param("appointmentId") Long appointmentId);

    /** api.md 13.5 预约详情中的处方摘要 */
    @Select("""
            SELECT id, appointment_id AS appointmentId, status,
                   total_amount AS totalAmount, create_time AS createTime
              FROM prescription
             WHERE appointment_id = #{appointmentId}
            """)
    PrescriptionBriefVO selectPrescriptionBrief(@Param("appointmentId") Long appointmentId);

    /** 开方时需要把患者 userId 写入处方，取自服务端数据库而不是请求体 */
    @Select("SELECT user_id FROM appointment WHERE id = #{appointmentId}")
    Long selectAppointmentUserId(@Param("appointmentId") Long appointmentId);
}
