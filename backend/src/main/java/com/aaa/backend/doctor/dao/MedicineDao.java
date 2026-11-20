package com.aaa.backend.doctor.dao;

import com.aaa.backend.doctor.vo.MedicineOptionVO;
import com.aaa.backend.entity.Medicine;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 医生开方用的药品数据入口。
 *
 * <p>既提供 BaseMapper 的按主键批量查询（完成接诊时服务端重新读取当前价格），
 * 也提供 api.md 13.10 的「启用且有库存」下拉查询。
 * 放在 doctor.dao 包下，复用 PatientMapperScanConfig 已有的扫描范围。
 */
public interface MedicineDao extends BaseMapper<Medicine> {

    @Select("""
            <script>
            SELECT m.id, m.name, m.spec, m.unit, m.price,
                   GREATEST(m.stock - COALESCE((
                       SELECT SUM(pi.quantity)
                         FROM prescription_item pi
                         JOIN prescription p ON p.id = pi.prescription_id
                        WHERE pi.medicine_id = m.id AND p.status IN ('unpaid', 'paid')
                   ), 0), 0) AS stock,
                   m.usage_desc AS usageDesc
              FROM medicine m
             WHERE m.status = 1 AND m.stock > COALESCE((
                       SELECT SUM(pi.quantity)
                         FROM prescription_item pi
                         JOIN prescription p ON p.id = pi.prescription_id
                        WHERE pi.medicine_id = m.id AND p.status IN ('unpaid', 'paid')
                   ), 0)
            <if test='keyword != null and keyword != ""'>
               AND (name LIKE CONCAT('%', #{keyword}, '%') OR spec LIKE CONCAT('%', #{keyword}, '%'))
            </if>
             ORDER BY id
             LIMIT 200
            </script>
            """)
    List<MedicineOptionVO> selectAvailable(@Param("keyword") String keyword);

    /** Lock an inventory row while checking or finalizing stock movements. */
    @Select("SELECT id, name, spec, unit, price, stock, usage_desc AS usageDesc, status FROM medicine WHERE id = #{id} FOR UPDATE")
    Medicine lockById(@Param("id") Long id);

    /** Active unpaid/paid prescription items reserve availability without changing physical stock. */
    @Select("""
            SELECT pi.quantity
              FROM prescription_item pi
              JOIN prescription p ON p.id = pi.prescription_id
             WHERE pi.medicine_id = #{medicineId}
               AND p.status IN ('unpaid', 'paid')
             FOR UPDATE
            """)
    List<Integer> selectReservedQuantitiesForUpdate(@Param("medicineId") Long medicineId);

    @Update("UPDATE medicine SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);
}
