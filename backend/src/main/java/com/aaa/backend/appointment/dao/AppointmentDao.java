package com.aaa.backend.appointment.dao;

import com.aaa.backend.appointment.vo.AppointmentDetailVO;
import com.aaa.backend.entity.Appointment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface AppointmentDao extends BaseMapper<Appointment> {

    /** Current locking read used to serialize patient state changes and their idempotency checks. */
    @Select("SELECT id, user_id AS userId, schedule_id AS scheduleId, status FROM appointment WHERE id = #{id} FOR UPDATE")
    AppointmentDetailVO lockByIdForUpdate(@Param("id") Long id);

    /**
     * 详情与列表共用的联表投影：医生、科室、排班、处方摘要、病历/评价存在性。
     * 定义为常量供多条 SQL 复用，保证列表和详情字段永远一致。
     */
    String DETAIL_COLUMNS = """
            SELECT a.id, a.user_id, a.schedule_id,
                   doc.id            AS doctor_id,
                   doc.name          AS doctor_name,
                   doc.title         AS doctor_title,
                   doc.avatar        AS doctor_avatar,
                   doc.department_id AS department_id,
                   dep.name          AS department_name,
                   dep.location      AS department_location,
                   s.schedule_date, s.time_slot,
                   a.patient_name, a.patient_phone, a.id_card, a.amount,
                   a.pay_method, a.pay_time, a.status, a.remark,
                   p.id              AS prescription_id,
                   p.status          AS prescription_status,
                   COALESCE(p.total_amount, 0.00) AS prescription_amount,
                   (EXISTS (SELECT 1 FROM consultation_record cr WHERE cr.appointment_id = a.id)) AS has_consultation,
                   (EXISTS (SELECT 1 FROM appointment_review ar WHERE ar.appointment_id = a.id)) AS evaluated,
                   a.create_time, a.update_time
              FROM appointment a
              JOIN schedule s ON s.id = a.schedule_id
              JOIN doctor doc ON doc.id = s.doctor_id
              LEFT JOIN department dep ON dep.id = doc.department_id
              LEFT JOIN prescription p ON p.appointment_id = a.id
            """;

    @Select("""
            SELECT a.check_in_time,
                   CASE WHEN a.status = 'checked_in' THEN
                     (SELECT COUNT(*) FROM appointment q
                       WHERE q.schedule_id = a.schedule_id AND q.status = 'checked_in'
                         AND (COALESCE(q.check_in_time, q.update_time), q.id)
                           < (COALESCE(a.check_in_time, a.update_time), a.id))
                   END AS waiting_ahead,
                   CASE WHEN a.status = 'checked_in' THEN
                     (SELECT COUNT(*) FROM appointment q
                       WHERE q.schedule_id = a.schedule_id AND q.status = 'in_consultation')
                   END AS in_consultation_count
              FROM appointment a WHERE a.id = #{id} AND a.user_id = #{userId}
            """)
    AppointmentDetailVO queueDetails(@Param("id") Long id, @Param("userId") Long userId);

    @Select(DETAIL_COLUMNS + " WHERE a.id = #{id} ")
    AppointmentDetailVO selectDetail(@Param("id") Long id);

    @Select("""
            <script>
            """ + DETAIL_COLUMNS + """
            WHERE a.user_id = #{userId}
            <if test='statuses != null and statuses.size() > 0'>
                AND a.status IN
                <foreach collection='statuses' item='item' open='(' separator=',' close=')'>#{item}</foreach>
            </if>
            <if test='pendingDispense'>
                AND EXISTS (SELECT 1 FROM prescription p2 WHERE p2.appointment_id = a.id AND p2.status = 'paid')
            </if>
             ORDER BY a.id DESC
             LIMIT #{offset}, #{size}
            </script>
            """)
    List<AppointmentDetailVO> selectMyPage(@Param("userId") Long userId,
                                           @Param("statuses") List<String> statuses,
                                           @Param("pendingDispense") boolean pendingDispense,
                                           @Param("offset") long offset,
                                           @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*)
              FROM appointment a
             WHERE a.user_id = #{userId}
            <if test='statuses != null and statuses.size() > 0'>
                AND a.status IN
                <foreach collection='statuses' item='item' open='(' separator=',' close=')'>#{item}</foreach>
            </if>
            <if test='pendingDispense'>
                AND EXISTS (SELECT 1 FROM prescription p2 WHERE p2.appointment_id = a.id AND p2.status = 'paid')
            </if>
            </script>
            """)
    long countMyPage(@Param("userId") Long userId,
                     @Param("statuses") List<String> statuses,
                     @Param("pendingDispense") boolean pendingDispense);

    /** 同一账号下的同一就诊人不可占用同一排班的多个号源。 */
    @Select("""
            SELECT COUNT(*) FROM appointment
             WHERE user_id = #{userId}
               AND schedule_id = #{scheduleId}
               AND status <> 'cancelled'
               AND (patient_profile_id = #{profileId}
                    OR (patient_name = #{name} AND patient_phone = #{phone}))
            """)
    long countActiveBySchedule(@Param("userId") Long userId, @Param("scheduleId") Long scheduleId,
                               @Param("profileId") Long profileId, @Param("name") String name,
                               @Param("phone") String phone);

    /** 支付：仅 unpaid 可流转，user_id 保证不能替别人付款。 */
    @Update("""
            UPDATE appointment
               SET status = 'pending', pay_method = #{payMethod}, pay_time = NOW()
             WHERE id = #{id}
               AND user_id = #{userId}
               AND status = 'unpaid'
            """)
    int markPaid(@Param("id") Long id,
                 @Param("userId") Long userId,
                 @Param("payMethod") String payMethod);

    /** 取消：仅 unpaid / pending（含历史 confirmed）可取消，重复取消影响行数为 0。 */
    @Update("""
            UPDATE appointment SET status = 'cancelled'
             WHERE id = #{id}
               AND user_id = #{userId}
               AND status IN ('unpaid', 'pending', 'confirmed')
            """)
    int cancel(@Param("id") Long id, @Param("userId") Long userId);

    /** 签到（严格）：仅 pending，且排班日期就是今天。 */
    @Update("""
            UPDATE appointment SET status = 'checked_in', check_in_time = NOW(6)
             WHERE id = #{id}
               AND user_id = #{userId}
               AND status IN ('pending', 'confirmed')
               AND EXISTS (
                    SELECT 1 FROM schedule s
                     WHERE s.id = appointment.schedule_id
                       AND s.schedule_date = CURDATE()
               )
            """)
    int checkInOnScheduleDate(@Param("id") Long id, @Param("userId") Long userId);

    /** 签到（宽松）：联调用，不校验就诊日期。 */
    @Update("""
            UPDATE appointment SET status = 'checked_in', check_in_time = NOW(6)
             WHERE id = #{id}
               AND user_id = #{userId}
               AND status IN ('pending', 'confirmed')
            """)
    int checkIn(@Param("id") Long id, @Param("userId") Long userId);
}
