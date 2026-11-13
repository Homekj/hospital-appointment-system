package com.aaa.backend.prescription.dao;

import com.aaa.backend.entity.PrescriptionItem;
import com.aaa.backend.prescription.vo.PrescriptionItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PrescriptionItemDao extends BaseMapper<PrescriptionItem> {

    /** 明细里的药名、规格、单价都是开方时的快照，直接读明细表，不回查药品库。 */
    @Select("""
            SELECT id, prescription_id, medicine_id, medicine_name, spec,
                   dosage, frequency, days, quantity, unit_price, amount
              FROM prescription_item
             WHERE prescription_id = #{prescriptionId}
             ORDER BY id
            """)
    List<PrescriptionItemVO> selectByPrescription(@Param("prescriptionId") Long prescriptionId);
}
