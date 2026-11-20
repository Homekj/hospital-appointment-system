package com.aaa.backend.prescription.dao;

import com.aaa.backend.entity.Prescription;
import com.aaa.backend.prescription.vo.PrescriptionDetailVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PrescriptionDao extends BaseMapper<Prescription> {

    /** 所有查询都带 user_id 条件，从 SQL 层面杜绝越权读取他人处方。 */
    String DETAIL_COLUMNS = """
            SELECT p.id, p.appointment_id, p.user_id, p.doctor_id,
                   doc.name AS doctor_name,
                   dep.name AS department_name,
                   p.total_amount, p.status, p.pay_method, p.pay_time,
                   p.create_time, p.update_time
              FROM prescription p
              LEFT JOIN doctor doc ON doc.id = p.doctor_id
              LEFT JOIN department dep ON dep.id = doc.department_id
            """;

    @Select(DETAIL_COLUMNS + " WHERE p.id = #{id} AND p.user_id = #{userId} ")
    PrescriptionDetailVO selectOwned(@Param("id") Long id, @Param("userId") Long userId);

    @Select(DETAIL_COLUMNS + " WHERE p.id = #{id} AND p.user_id = #{userId} FOR UPDATE")
    PrescriptionDetailVO lockOwnedForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    /** 一个预约最多一张处方（uk_prescription_appt），所以直接返回单条。 */
    @Select(DETAIL_COLUMNS + " WHERE p.appointment_id = #{appointmentId} AND p.user_id = #{userId} ")
    PrescriptionDetailVO selectByAppointment(@Param("appointmentId") Long appointmentId,
                                             @Param("userId") Long userId);

    /** 条件更新抢状态：并发下只有一个请求能拿到影响行数 1，杜绝重复扣款。 */
    @Update("""
            UPDATE prescription
               SET status = 'paid', pay_method = #{payMethod}, pay_time = NOW()
             WHERE id = #{id}
               AND user_id = #{userId}
               AND status = 'unpaid'
            """)
    int markPaid(@Param("id") Long id,
                 @Param("userId") Long userId,
                 @Param("payMethod") String payMethod);

    @Update("UPDATE prescription SET status = 'dispensed' WHERE id = #{id} AND user_id = #{userId} AND status = 'paid'")
    int markDispensed(@Param("id") Long id, @Param("userId") Long userId);
}
