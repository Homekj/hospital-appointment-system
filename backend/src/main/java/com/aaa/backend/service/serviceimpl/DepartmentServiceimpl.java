package com.aaa.backend.service.serviceimpl;

import com.aaa.backend.common.Result;
import com.aaa.backend.dao.DepartmentDao;
import com.aaa.backend.dao.DoctorDao;
import com.aaa.backend.entity.Department;
import com.aaa.backend.entity.Doctor;
import com.aaa.backend.service.DepartmentService;
import com.aaa.backend.vo.DepartmentQuery;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceimpl implements DepartmentService {
    @Autowired
    private DepartmentDao departmentDao;
    @Autowired
    private DoctorDao doctorDao;

    @Override
    public Result getByLikeName(String name) {
        QueryWrapper<Department> queryWrapper = new QueryWrapper<>();
        // 与医生模块保持一致：不传/空值时不加条件，返回全部
        if (name != null && !name.trim().isEmpty()) {
            queryWrapper.like("name", name.trim());
        }
        List<Department> list = departmentDao.selectList(queryWrapper);
        return Result.ok(list);
    }

    @Override
    public Result addDepartment(Department department) {
        if (department == null || department.getName() == null || department.getName().trim().isEmpty()) {
            return Result.error("科室名称不能为空");
        }
        department.setName(department.getName().trim());
        if (nameExists(department.getName(), null)) {
            return Result.error("科室名称已存在");
        }
        if (department.getStatus() == null) {
            department.setStatus(1);
        }
        if (department.getSortOrder() == null) {
            department.setSortOrder(0);
        }
        department.setId(null);
        return departmentDao.insert(department) > 0
                ? Result.ok("添加成功")
                : Result.error("添加失败");
    }

    @Override
    public Result updateDepartment(Department department) {
        if (department == null || department.getId() == null) {
            return Result.error("修改失败，ID不能为空");
        }
        if (department.getName() == null || department.getName().trim().isEmpty()) {
            return Result.error("科室名称不能为空");
        }
        department.setName(department.getName().trim());
        if (departmentDao.selectById(department.getId()) == null) {
            return Result.error("修改失败，数据不存在");
        }
        if (nameExists(department.getName(), department.getId())) {
            return Result.error("科室名称已存在");
        }
        return departmentDao.updateById(department) > 0
                ? Result.ok("修改成功")
                : Result.error("修改失败，数据不存在");
    }

    @Override
    public Result deleteDepartment(Long id) {
        if (id == null) {
            return Result.error("科室ID不能为空");
        }
        if (departmentDao.selectById(id) == null) {
            return Result.error("科室不存在");
        }
        QueryWrapper<Doctor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("department_id", id);
        Long doctorCount = doctorDao.selectCount(queryWrapper);
        if (doctorCount != null && doctorCount > 0) {
            return Result.error("该科室下有关联医生，无法删除");
        }
        // 真正执行删除，避免「返回成功但记录仍在」
        return departmentDao.deleteById(id) > 0
                ? Result.ok("删除成功")
                : Result.error("删除失败");
    }

    @Override
    public Result getByDepartmentName() {
        // 返回精简 VO，不再把实体里未查询的字段以默认值（如 status=1）返回给前端
        return Result.ok(departmentDao.listEnabledOptions());
    }

    // ------------------------------------------------------------------
    // 以下三个方法服务于 api.md 12.2 的 /api/admin/departments 契约，
    // 全部返回 MyBatis-Plus IPage / 实体，由 ResultBridge 统一转成文档响应。
    // ------------------------------------------------------------------

    @Override
    public Result page(DepartmentQuery query) {
        DepartmentQuery condition = query == null ? new DepartmentQuery() : query;
        int pageNum = condition.getPageNum() == null || condition.getPageNum() < 1
                ? 1 : condition.getPageNum();
        int pageSize = condition.getPageSize() == null || condition.getPageSize() < 1
                ? 10 : Math.min(condition.getPageSize(), 100);

        QueryWrapper<Department> wrapper = new QueryWrapper<>();
        if (condition.getKeyword() != null && !condition.getKeyword().trim().isEmpty()) {
            wrapper.like("name", condition.getKeyword().trim());
        }
        if (condition.getStatus() != null) {
            if (condition.getStatus() != 0 && condition.getStatus() != 1) {
                return Result.error("status 只能是 0(停用) 或 1(启用)");
            }
            wrapper.eq("status", condition.getStatus());
        }
        wrapper.orderByAsc("sort_order").orderByAsc("id");

        IPage<Department> result = departmentDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.ok(result);
    }

    @Override
    public Result detail(Long id) {
        if (id == null) {
            return Result.error("科室ID不能为空");
        }
        Department department = departmentDao.selectById(id);
        if (department == null) {
            return Result.error("科室不存在");
        }
        return Result.ok(department);
    }

    @Override
    public Result changeStatus(Long id, Integer status) {
        if (id == null) {
            return Result.error("科室ID不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("status 只能是 0(停用) 或 1(启用)");
        }
        if (departmentDao.selectById(id) == null) {
            return Result.error("科室不存在");
        }
        // api.md 12.2：停用前确认没有未完成预约，避免患者端出现「有号却查不到医生」
        if (status == 0) {
            Long active = departmentDao.countActiveAppointments(id);
            if (active != null && active > 0) {
                return Result.error("该科室已存在未完成预约，无法停用");
            }
        }
        Department patch = new Department();
        patch.setId(id);
        patch.setStatus(status);
        return departmentDao.updateById(patch) > 0
                ? Result.ok(status == 1 ? "已启用" : "已停用")
                : Result.error("状态更新失败");
    }

    /** 科室名唯一性校验；excludeId 非空时排除自身（修改场景） */
    private boolean nameExists(String name, Long excludeId) {
        QueryWrapper<Department> query = new QueryWrapper<>();
        query.eq("name", name);
        if (excludeId != null) {
            query.ne("id", excludeId);
        }
        Long count = departmentDao.selectCount(query);
        return count != null && count > 0;
    }
}
