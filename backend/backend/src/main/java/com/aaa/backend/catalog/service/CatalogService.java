package com.aaa.backend.catalog.service;

import com.aaa.backend.catalog.dao.DepartmentDao;
import com.aaa.backend.catalog.dao.DoctorDao;
import com.aaa.backend.catalog.dao.ScheduleDao;
import com.aaa.backend.catalog.vo.DepartmentVO;
import com.aaa.backend.catalog.vo.DoctorVO;
import com.aaa.backend.catalog.vo.ScheduleVO;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.entity.Department;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/** 公开查询域：科室、医生、排班均不需要登录，也不涉及任何写操作。 */
@Service
public class CatalogService {

    private final DepartmentDao departmentDao;
    private final DoctorDao doctorDao;
    private final ScheduleDao scheduleDao;

    public CatalogService(DepartmentDao departmentDao, DoctorDao doctorDao, ScheduleDao scheduleDao) {
        this.departmentDao = departmentDao;
        this.doctorDao = doctorDao;
        this.scheduleDao = scheduleDao;
    }

    /** 只返回启用科室，按 sortOrder、id 升序；keyword 为空时不拼模糊条件。 */
    public List<DepartmentVO> departments(String keyword) {
        LambdaQueryWrapper<Department> query = new LambdaQueryWrapper<>();
        query.eq(Department::getStatus, 1)
                .like(hasText(keyword), Department::getName, trimToNull(keyword))
                .orderByAsc(Department::getSortOrder, Department::getId);
        return departmentDao.selectList(query).stream().map(department -> {
            DepartmentVO vo = new DepartmentVO();
            vo.setId(department.getId());
            vo.setName(department.getName());
            vo.setDescription(department.getDescription());
            vo.setLocation(department.getLocation());
            vo.setSortOrder(department.getSortOrder());
            vo.setStatus(department.getStatus());
            return vo;
        }).toList();
    }

    public List<DoctorVO> doctors(Long departmentId, String keyword, Boolean hasAvailableSlot) {
        return doctorDao.selectDoctors(null, departmentId, trimToNull(keyword),
                Boolean.TRUE.equals(hasAvailableSlot));
    }

    /** 医生不存在或已停用统一返回 404。 */
    public DoctorVO doctor(Long id) {
        List<DoctorVO> result = doctorDao.selectDoctors(id, null, null, false);
        if (result.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "医生不存在或已停用");
        }
        return result.get(0);
    }

    public List<ScheduleVO> schedules(Long doctorId, LocalDate scheduleDate,
                                      LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate 不能晚于 endDate");
        }
        return scheduleDao.selectAvailableSchedules(doctorId, scheduleDate, startDate, endDate);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
