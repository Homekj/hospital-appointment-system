package com.aaa.backend.dao;

import com.aaa.backend.entity.Doctor;
import com.aaa.backend.vo.DoctorQuery; // 加上这个
import com.aaa.backend.vo.DoctorVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param; // 换成这个

@Mapper // 建议加上这个注解，防止启动报错
public interface DoctorDao extends BaseMapper<Doctor> {

    IPage<DoctorVo> pageDoctorWithDept(Page<?> page, @Param("query") DoctorQuery query);

}