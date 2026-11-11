package com.aaa.backend.dao;

import com.aaa.backend.entity.Department;
import com.aaa.backend.vo.OptionVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DepartmentDao extends BaseMapper<Department> {

    /** 下拉框只查启用科室的 id / name，返回专用 VO 避免实体默认值污染 */
    @Select("SELECT id, name FROM department WHERE status = 1 ORDER BY sort_order, id")
    List<OptionVo> listEnabledOptions();
}
