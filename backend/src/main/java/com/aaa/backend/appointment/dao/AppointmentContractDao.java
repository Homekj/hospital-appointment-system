package com.aaa.backend.appointment.dao;

import com.aaa.backend.appointment.vo.AppointmentSummaryRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 供 admin / doctor 域使用的预约查询与状态流转 SQL。
 *
 * 本接口只负责 SQL，业务规则与事务边界在 AppointmentContractService。
 * 所有状态变更一律用「带条件的 UPDATE + 检查影响行数」，与患者侧保持同一套并发策略。
 */
public interface AppointmentContractDao {

    /** Serialize appointment contract commands before reading current status. */
    @Select("SELECT status FROM appointment WHERE id = #{id} FOR UPDATE")
    String lockStatusForUpdate(@Param("id") Long id);

    /** 列表与详情共用的联表投影，保证两个入口字段完全一致。 */
    String SUMMARY_COLUMNS = """
            SELECT a.id, a.user_id, a.schedule_id,
                   doc.id            AS doctor_id,
                   doc.name          AS doctor_name,
                   doc.title         AS doctor_title,
                   doc.department_id AS department_id,
                   dep.name          AS department_name,
                   s.schedule_date, s.time_slot,
                   a.patient_name, a.patient_phone, a.amount,
                   a.pay_method, a.pay_time, a.status, a.remark,
                   a.create_time, a.update_time
              FROM appointment a
              JOIN schedule s ON s.id = a.schedule_id
              JOIN doctor doc ON doc.id = s.doctor_id
              LEFT JOIN department dep ON dep.id = doc.department_id
             WHERE 1 = 1
            """;

    /** 列表筛选片段；startDate / endDate 按就诊日期(schedule_date)过滤。 */
    String FILTERS = """
            <if test='statuses != null and statuses.size() > 0'>
                AND a.status IN
                <foreach collection='statuses' item='item' open='(' separator=',' close=')'>#{item}</foreach>
            </if>
            <if test='doctorId != null'> AND doc.id = #{doctorId} </if>
            <if test='departmentId != null'> AND doc.department_id = #{departmentId} </if>
            <if test='patientName != null and patientName.length() > 0'>
                AND a.patient_name LIKE CONCAT('%', #{patientName}, '%')
            </if>
            <if test='startDate != null'> AND s.schedule_date >= #{startDate} </if>
            <if test='endDate != null'> AND #{endDate} >= s.schedule_date </if>
            <if test='scheduleDate != null'> AND s.schedule_date = #{scheduleDate} </if>
            """;

    @Select("<script>" + SUMMARY_COLUMNS + FILTERS
            + " ORDER BY a.id DESC LIMIT #{offset}, #{size} </script>")
    List<AppointmentSummaryRow> selectPage(@Param("statuses") List<String> statuses,
                                           @Param("doctorId") Long doctorId,
                                           @Param("departmentId") Long departmentId,
                                           @Param("patientName") String patientName,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("scheduleDate") LocalDate scheduleDate,
                                           @Param("offset") long offset,
                                           @Param("size") int size);

    @Select("<script> SELECT COUNT(*) FROM appointment a "
            + " JOIN schedule s ON s.id = a.schedule_id "
            + " JOIN doctor doc ON doc.id = s.doctor_id "
            + " LEFT JOIN department dep ON dep.id = doc.department_id "
            + " WHERE 1 = 1 " + FILTERS + " </script>")
    long countPage(@Param("statuses") List<String> statuses,
                   @Param("doctorId") Long doctorId,
                   @Param("departmentId") Long departmentId,
                   @Param("patientName") String patientName,
                   @Param("startDate") LocalDate startDate,
                   @Param("endDate") LocalDate endDate,
                   @Param("scheduleDate") LocalDate scheduleDate);

    /** doctorId 为 null 表示管理员视角；非 null 时强制限定该医生的排班，防止越权查看他人预约。 */
    @Select("<script>" + SUMMARY_COLUMNS
            + " AND a.id = #{id} "
            + "<if test='doctorId != null'> AND doc.id = #{doctorId} </if>"
            + " </script>")
    AppointmentSummaryRow selectDetail(@Param("id") Long id, @Param("doctorId") Long doctorId);

    /** 完成接诊前的病历存在性检查，以及失败时用于区分错误原因。 */
    @Select("SELECT COUNT(*) FROM consultation_record WHERE appointment_id = #{id}")
    long countConsultation(@Param("id") Long id);

    /** 管理员取消：仅未支付/已支付/已签到可取消；checked_in 之后的状态不允许。 */
    @Update("""
            UPDATE appointment SET status = 'cancelled'
             WHERE id = #{id}
               AND status IN ('unpaid', 'pending', 'confirmed', 'checked_in')
            """)
    int cancelByAdmin(@Param("id") Long id);

    /** 开始接诊：checked_in -> in_consultation，且必须由该排班所属医生操作。 */
    @Update("""
            UPDATE appointment a
               SET a.status = 'in_consultation'
             WHERE a.id = #{id}
               AND a.status = 'checked_in'
               AND EXISTS (
                    SELECT 1 FROM schedule s
                     WHERE s.id = a.schedule_id AND s.doctor_id = #{doctorId}
               )
            """)
    int startConsultation(@Param("id") Long id, @Param("doctorId") Long doctorId);

    /**
     * 完成接诊：in_consultation -> pending_rx_pay（有未取消处方）或 completed（无处方）。
     * 三个前置条件都写进 WHERE：状态、医生归属、必须已有病历，避免并发下出现非法流转。
     */
    @Update("""
            UPDATE appointment a
               SET a.status = CASE WHEN EXISTS (
                        SELECT 1 FROM prescription p
                         WHERE p.appointment_id = a.id AND p.status != 'cancelled'
                    ) THEN 'pending_rx_pay' ELSE 'completed' END
             WHERE a.id = #{id}
               AND a.status = 'in_consultation'
               AND EXISTS (
                    SELECT 1 FROM schedule s
                     WHERE s.id = a.schedule_id AND s.doctor_id = #{doctorId}
               )
               AND EXISTS (
                    SELECT 1 FROM consultation_record cr WHERE cr.appointment_id = a.id
               )
            """)
    int completeConsultation(@Param("id") Long id, @Param("doctorId") Long doctorId);

    /** Finish the prescription stage only after the admin action has changed the prescription state. */
    @Update("""
            UPDATE appointment a
               SET a.status = 'completed'
             WHERE a.id = #{id}
               AND a.status = 'pending_rx_pay'
               AND EXISTS (
                    SELECT 1 FROM prescription p
                     WHERE p.appointment_id = a.id AND p.status = #{prescriptionStatus}
               )
            """)
    int completeAfterPrescriptionAction(@Param("id") Long id,
                                        @Param("prescriptionStatus") String prescriptionStatus);
}
