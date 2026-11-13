package com.aaa.backend.service.serviceimpl;

import com.aaa.backend.common.Result;
import com.aaa.backend.dao.DepartmentDao;
import com.aaa.backend.dao.DoctorDao;
import com.aaa.backend.dao.UserDao;
import com.aaa.backend.entity.Department;
import com.aaa.backend.entity.Doctor;
import com.aaa.backend.entity.SysUser;
import com.aaa.backend.service.DoctorService;
import com.aaa.backend.vo.DoctorCreateVo;
import com.aaa.backend.vo.DoctorQuery;
import com.aaa.backend.vo.DoctorVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DoctorServiceimpl implements DoctorService {
    @Autowired
    private DoctorDao doctorDao;
    @Autowired
    private DepartmentDao departmentDao;
    @Autowired
    private UserDao userDao;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Result getByLikeName(String name) {
        QueryWrapper<Doctor> queryWrapper = new QueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            queryWrapper.like("name", name.trim());
        }
        List<Doctor> list = doctorDao.selectList(queryWrapper);
        return Result.ok(list);
    }

    @Override
    public Result page(DoctorQuery query) {
        DoctorQuery condition = query == null ? new DoctorQuery() : query;
        // 兼容前端统一搜索框 keyword
        if ((condition.getName() == null || condition.getName().trim().isEmpty())
                && condition.getKeyword() != null && !condition.getKeyword().trim().isEmpty()) {
            condition.setName(condition.getKeyword().trim());
        }
        int pageNum = condition.getPageNum() == null || condition.getPageNum() < 1 ? 1 : condition.getPageNum();
        int pageSize = condition.getPageSize() == null || condition.getPageSize() < 1
                ? 10 : Math.min(condition.getPageSize(), 100);
        condition.setPageNum(pageNum);
        condition.setPageSize(pageSize);

        Page<DoctorVo> page = new Page<>(pageNum, pageSize);
        IPage<DoctorVo> result = doctorDao.pageDoctorWithDept(page, condition);
        return Result.ok(result);
    }

    @Override
    public Result detail(Long id) {
        if (id == null) {
            return Result.error("医生ID不能为空");
        }
        DoctorVo vo = doctorDao.selectDoctorVoById(id);
        if (vo == null) {
            return Result.error("医生不存在");
        }
        return Result.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result add(Doctor doctor) {
        // 1. 参数校验
        String error = validate(doctor, false);
        if (error != null) {
            return Result.error(error);
        }
        doctor.setName(doctor.getName().trim());

        // 2. 校验科室是否存在且启用（null-safe，避免 getStatus() 为 null 时拆箱 NPE）
        Department dept = departmentDao.selectById(doctor.getDepartmentId());
        if (dept == null || !Integer.valueOf(1).equals(dept.getStatus())) {
            return Result.error("所属科室不存在或已停用");
        }

        // 3. 处理默认值
        if (doctor.getGender() == null) doctor.setGender(1);
        if (doctor.getStatus() == null) doctor.setStatus(1);
        doctor.setId(null); // 强制置空，让数据库自增

        // 4. 插入医生表（插入成功后 doctor.getId() 自动拿到新 ID）
        if (doctorDao.insert(doctor) <= 0) {
            return Result.error("添加失败");
        }

        // 5. 账号联动：按姓名拼音首字母生成不冲突的登录账号
        String initial = getPinyinInitial(doctor.getName());
        String baseUsername = "doctor" + initial;
        String finalUsername = baseUsername;
        int suffix = 1;
        while (true) {
            QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
            wrapper.eq("username", finalUsername);
            Long count = userDao.selectCount(wrapper);
            if (count == null || count == 0) {
                break;
            }
            finalUsername = baseUsername + suffix;
            suffix++;
        }

        SysUser user = new SysUser();
        user.setUsername(finalUsername);
        user.setPassword(passwordEncoder.encode("123"));
        user.setNickname(doctor.getName());
        user.setDoctorId(doctor.getId());
        user.setRole("DOCTOR");
        user.setStatus(1);
        user.setDeleted(0);
        user.setBalance(BigDecimal.ZERO);
        userDao.insert(user);

        // 6. 返回新增医生 id 与自动生成的账号信息，前端不再只能拿到一句提示
        String message = "添加成功，自动生成登录账号：" + finalUsername + "，默认密码：123";
        return Result.ok(new DoctorCreateVo(doctor.getId(), finalUsername, "123", message));
    }

    /**
     * 辅助方法：提取汉字的拼音首字母并转小写
     * 例如："张三" -> "zs" ; "王小明" -> "wxm"；英文字母也统一转小写
     */
    private String getPinyinInitial(String name) {
        StringBuilder pinyinStr = new StringBuilder();
        char[] newChar = name.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

        for (char c : newChar) {
            // 英文字母与数字直接保留（统一转小写），避免 "测试2" 里的数字被吞掉
            if (String.valueOf(c).matches("^[a-zA-Z0-9]+$")) {
                pinyinStr.append(Character.toLowerCase(c));
            } else {
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, defaultFormat);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        pinyinStr.append(Character.toLowerCase(pinyinArray[0].charAt(0)));
                    }
                } catch (Exception e) {
                    // 忽略异常，比如生僻字无法转换
                }
            }
        }
        return pinyinStr.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result update(Doctor doctor) {
        String error = validate(doctor, true);
        if (error != null) {
            return Result.error(error);
        }
        doctor.setName(doctor.getName().trim());
        Doctor before = doctorDao.selectById(doctor.getId());
        if (before == null) {
            return Result.error("医生不存在");
        }
        // 时间戳由数据库维护，禁止前端覆盖
        doctor.setCreateTime(null);
        doctor.setUpdateTime(null);

        if (doctorDao.updateById(doctor) <= 0) {
            return Result.error("修改失败");
        }

        // 改名时联动账号昵称，避免 doctor.name 与 sys_user.nickname 不一致
        if (doctor.getName() != null && !doctor.getName().equals(before.getName())) {
            SysUser patch = new SysUser();
            patch.setNickname(doctor.getName().trim());
            QueryWrapper<SysUser> userWrapper = new QueryWrapper<>();
            userWrapper.eq("doctor_id", doctor.getId());
            userDao.update(patch, userWrapper);
        }
        return Result.ok("修改成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result delete(Long id) {
        if (id == null) {
            return Result.error("医生ID不能为空");
        }
        if (doctorDao.selectById(id) == null) {
            return Result.error("医生不存在");
        }

        // 1. 检查是否有排班记录（如果有，坚决不让删）
        Long scheduleCount = doctorDao.countScheduleByDoctorId(id);
        if (scheduleCount != null && scheduleCount > 0) {
            return Result.error("该医生存在排班记录，无法删除，请先停用");
        }

        // 2. 删除医生表的数据
        if (doctorDao.deleteById(id) > 0) {
            // 3. 联动删除 sys_user 表里的账号数据
            QueryWrapper<SysUser> userWrapper = new QueryWrapper<>();
            userWrapper.eq("doctor_id", id);
            userDao.delete(userWrapper);
            return Result.ok("删除成功");
        }
        return Result.error("删除失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result changeStatus(Long id, Integer status) {
        if (id == null) {
            return Result.error("医生ID不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("状态只能是 0(停用) 或 1(启用)");
        }
        if (doctorDao.selectById(id) == null) {
            return Result.error("医生不存在");
        }
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setStatus(status);
        if (doctorDao.updateById(doctor) <= 0) {
            return Result.error("状态更新失败");
        }

        // 关键联动：停用医生必须同时禁用其登录账号，
        // 否则 LoginServiceimpl 仍会为其签发 token、已有 token 也依然有效。
        SysUser patch = new SysUser();
        patch.setStatus(status);
        QueryWrapper<SysUser> userWrapper = new QueryWrapper<>();
        userWrapper.eq("doctor_id", id);
        userDao.update(patch, userWrapper);

        return Result.ok(status == 1 ? "已启用" : "已停用");
    }

    /** 新增/修改共用校验；修改时额外要求 ID */
    private String validate(Doctor doctor, boolean requireId) {
        if (doctor == null) {
            return "请求参数不能为空";
        }
        if (requireId && doctor.getId() == null) {
            return "医生ID不能为空";
        }
        if (doctor.getName() == null || doctor.getName().trim().isEmpty()) {
            return "医生姓名不能为空";
        }
        if (doctor.getDepartmentId() == null) {
            return "所属科室不能为空";
        }
        Department department = departmentDao.selectById(doctor.getDepartmentId());
        if (department == null) {
            return "所属科室不存在";
        }
        if (doctor.getGender() != null && doctor.getGender() != 1 && doctor.getGender() != 2) {
            return "性别只能是 1(男) 或 2(女)";
        }
        if (doctor.getStatus() != null && doctor.getStatus() != 0 && doctor.getStatus() != 1) {
            return "状态只能是 0(停用) 或 1(启用)";
        }
        return null;
    }

    @Override
    public Result listOptions() {
        // 返回 [{id, name}]，不再把 Doctor 实体里未查询的字段以 null 返回
        return Result.ok(doctorDao.listEnabledOptions());
    }
}
