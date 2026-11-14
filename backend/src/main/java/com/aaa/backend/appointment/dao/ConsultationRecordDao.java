package com.aaa.backend.appointment.dao;

import com.aaa.backend.appointment.vo.ConsultationVO;
import com.aaa.backend.entity.ConsultationRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ConsultationRecordDao extends BaseMapper<ConsultationRecord> {

    /** 病历按 appointment_id 唯一（uk_consultation_appt），联医生表补出医生姓名。 */
    @Select("""
            SELECT cr.id, cr.appointment_id, cr.doctor_id,
                   doc.name AS doctor_name,
                   cr.chief_complaint, cr.present_illness, cr.diagnosis,
                   cr.treatment_plan, cr.doctor_advice,
                   cr.create_time, cr.update_time
              FROM consultation_record cr
              LEFT JOIN doctor doc ON doc.id = cr.doctor_id
             WHERE cr.appointment_id = #{appointmentId}
            """)
    ConsultationVO selectByAppointment(@Param("appointmentId") Long appointmentId);
}
