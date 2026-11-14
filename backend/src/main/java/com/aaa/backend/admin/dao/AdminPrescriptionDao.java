package com.aaa.backend.admin.dao;

import com.aaa.backend.prescription.vo.PrescriptionDetailVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface AdminPrescriptionDao {

    /** Serialize dispense/cancel actions before any stock or appointment changes. */
    @Select("SELECT status FROM prescription WHERE id = #{id} FOR UPDATE")
    String lockStatusForUpdate(@Param("id") Long id);

    String DETAIL_COLUMNS = """
            SELECT p.id, p.appointment_id AS appointmentId, p.user_id AS userId,
                   p.doctor_id AS doctorId, doc.name AS doctorName,
                   dep.name AS departmentName, p.total_amount AS totalAmount,
                   p.status, p.pay_method AS payMethod, p.pay_time AS payTime,
                   p.create_time AS createTime, p.update_time AS updateTime
              FROM prescription p
              LEFT JOIN doctor doc ON doc.id = p.doctor_id
              LEFT JOIN department dep ON dep.id = doc.department_id
            """;

    @Select("<script>" + DETAIL_COLUMNS + " WHERE 1 = 1 "
            + "<if test='status != null and status != &quot;&quot;'> AND p.status = #{status} </if>"
            + "<if test='userId != null'> AND p.user_id = #{userId} </if>"
            + "<if test='doctorId != null'> AND p.doctor_id = #{doctorId} </if>"
            + " ORDER BY p.id DESC LIMIT #{offset}, #{size} </script>")
    List<PrescriptionDetailVO> selectPage(@Param("status") String status,
                                          @Param("userId") Long userId,
                                          @Param("doctorId") Long doctorId,
                                          @Param("offset") long offset,
                                          @Param("size") int size);

    @Select("<script>SELECT COUNT(*) FROM prescription p WHERE 1 = 1 "
            + "<if test='status != null and status != &quot;&quot;'> AND p.status = #{status} </if>"
            + "<if test='userId != null'> AND p.user_id = #{userId} </if>"
            + "<if test='doctorId != null'> AND p.doctor_id = #{doctorId} </if></script>")
    long countPage(@Param("status") String status,
                   @Param("userId") Long userId,
                   @Param("doctorId") Long doctorId);

    @Select(DETAIL_COLUMNS + " WHERE p.id = #{id}")
    PrescriptionDetailVO selectDetail(@Param("id") Long id);

    @Update("UPDATE prescription SET status = 'dispensed' WHERE id = #{id} AND status = 'paid'")
    int markDispensed(@Param("id") Long id);

    @Update("UPDATE prescription SET status = 'cancelled' WHERE id = #{id} AND status = 'unpaid'")
    int cancelUnpaid(@Param("id") Long id);
}
