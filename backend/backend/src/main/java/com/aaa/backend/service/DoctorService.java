package com.aaa.backend.service;

import com.aaa.backend.common.Result;
import com.aaa.backend.entity.Doctor;
import com.aaa.backend.vo.DoctorQuery;

public interface DoctorService {
    /** 保留旧接口：按姓名模糊查询 */
    Result getByLikeName(String name);

    /** 分页查询（支持姓名、科室、状态筛选） */
    Result page(DoctorQuery query);

    /** 医生详情 */
    Result detail(Long id);

    /** 新增医生 */
    Result add(Doctor doctor);

    /** 修改医生 */
    Result update(Doctor doctor);

    /** 删除医生（存在关联排班时禁止删除） */
    Result delete(Long id);

    /** 启用 / 停用 */
    Result changeStatus(Long id, Integer status);
    Result listOptions();
}
