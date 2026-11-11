package com.aaa.backend.catalog.dao;

import com.aaa.backend.catalog.vo.DoctorVO;
import com.aaa.backend.entity.Doctor;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DoctorDao extends BaseMapper<Doctor> {

    /**
     * 医生列表与详情共用一条 SQL：id 非空时即为详情查询。
     * rating / reviewCount / consultCount 用相关子查询一次聚合，避免 Java 侧 N+1 查询。
     * hasAvailableSlot 依据未来 7 天是否存在「正常且有剩余号源」的排班。
     */
    @Select("""
            <script>
            SELECT doc.id,
                   doc.department_id,
                   dep.name                AS department_name,
                   doc.name,
                   doc.title,
                   doc.gender,
                   doc.avatar,
                   doc.intro,
                   doc.status,
                   (SELECT ROUND(AVG(ar.rating), 1)
                      FROM appointment_review ar
                     WHERE ar.doctor_id = doc.id) AS rating,
                   (SELECT COUNT(*)
                      FROM appointment_review ar
                     WHERE ar.doctor_id = doc.id) AS review_count,
                   (SELECT COUNT(*)
                      FROM appointment a
                      JOIN schedule s ON s.id = a.schedule_id
                     WHERE s.doctor_id = doc.id
                       AND a.status IN ('completed')) AS consult_count,
                   (CASE WHEN EXISTS (
                        SELECT 1 FROM schedule s
                         WHERE s.doctor_id = doc.id
                           AND s.status = 1
                           AND s.schedule_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)
                           AND s.total_quota > s.booked_count
                    ) THEN 1 ELSE 0 END) AS has_available_slot
              FROM doctor doc
              LEFT JOIN department dep ON dep.id = doc.department_id
             WHERE doc.status = 1
            <if test='id != null'> AND doc.id = #{id} </if>
            <if test='departmentId != null'> AND doc.department_id = #{departmentId} </if>
            <if test='keyword != null and keyword.length() > 0'>
               AND (doc.name LIKE CONCAT('%', #{keyword}, '%') OR doc.title LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='onlyAvailable'>
               AND EXISTS (
                    SELECT 1 FROM schedule s
                     WHERE s.doctor_id = doc.id
                       AND s.status = 1
                       AND s.schedule_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)
                       AND s.total_quota > s.booked_count
               )
            </if>
             ORDER BY doc.id
            </script>
            """)
    List<DoctorVO> selectDoctors(@Param("id") Long id,
                                 @Param("departmentId") Long departmentId,
                                 @Param("keyword") String keyword,
                                 @Param("onlyAvailable") boolean onlyAvailable);
}
