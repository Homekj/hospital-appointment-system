package com.aaa.backend.dao;

import com.aaa.backend.entity.Doctor;
import com.aaa.backend.vo.DoctorQuery;
import com.aaa.backend.vo.DoctorVo;
import com.aaa.backend.vo.OptionVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DoctorDao extends BaseMapper<Doctor> {

    /** 分页联表查询医生列表（含科室名称、绑定账号），SQL 见 DoctorMapper.xml */
    IPage<DoctorVo> pageDoctorWithDept(Page<?> page, @Param("query") DoctorQuery query);

    /** 医生详情，含科室名称与绑定账号 */
    DoctorVo selectDoctorVoById(@Param("id") Long id);

    /** 删除前检查是否存在关联排班，避免误删已有号源的医生 */
    @Select("SELECT COUNT(*) FROM schedule WHERE doctor_id = #{doctorId}")
    Long countScheduleByDoctorId(@Param("doctorId") Long doctorId);

    /** 下拉框只查启用医生的 id / name，返回专用 VO 避免实体默认值污染 */
    @Select("SELECT id, name FROM doctor WHERE status = 1 ORDER BY id")
    List<OptionVo> listEnabledOptions();

}
