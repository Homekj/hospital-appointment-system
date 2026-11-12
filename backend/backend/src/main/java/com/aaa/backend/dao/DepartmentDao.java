package com.aaa.backend.dao;

import com.aaa.backend.entity.Department;
import com.aaa.backend.vo.OptionVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DepartmentDao extends BaseMapper<Department> {

    /** 下拉框只查启用科室的 id / name，返回专用 VO 避免实体默认值污染 */
    @Select("SELECT id, name FROM department WHERE status = 1 ORDER BY sort_order, id")
    List<OptionVo> listEnabledOptions();

    /**
     * api.md 12.2「停用科室前应确认该科室没有未完成预约」。
     * 通过 预约 → 排班 → 医生 → 科室 四层关联统计，仍在进行中的预约才计入。
     */
    @Select("""
            SELECT COUNT(*)
              FROM appointment a
              JOIN schedule s ON s.id = a.schedule_id
              JOIN doctor d   ON d.id = s.doctor_id
             WHERE d.department_id = #{departmentId}
               AND a.status IN ('unpaid', 'pending', 'checked_in',
                                'in_consultation', 'pending_rx_pay')
            """)
    Long countActiveAppointments(@Param("departmentId") Long departmentId);
}
