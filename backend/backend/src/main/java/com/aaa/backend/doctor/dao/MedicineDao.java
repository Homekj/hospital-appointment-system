package com.aaa.backend.doctor.dao;

import com.aaa.backend.doctor.vo.MedicineOptionVO;
import com.aaa.backend.entity.Medicine;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
            SELECT id, name, spec, unit, price, stock, usage_desc AS usageDesc
              FROM medicine
             WHERE status = 1 AND stock &gt; 0
            <if test='keyword != null and keyword != ""'>
               AND (name LIKE CONCAT('%', #{keyword}, '%') OR spec LIKE CONCAT('%', #{keyword}, '%'))
            </if>
             ORDER BY id
             LIMIT 200
            </script>
            """)
    List<MedicineOptionVO> selectAvailable(@Param("keyword") String keyword);
}
