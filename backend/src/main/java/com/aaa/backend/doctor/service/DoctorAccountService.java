package com.aaa.backend.doctor.service;

import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.dao.UserDao;
import com.aaa.backend.dao.DoctorDao;
import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.doctor.dto.DoctorPasswordUpdateRequest;
import com.aaa.backend.entity.Doctor;
import com.aaa.backend.entity.SysUser;
import com.aaa.backend.file.FileStorageService;
import com.aaa.backend.service.LoginService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class DoctorAccountService {
    private final UserDao userDao;
    private final DoctorDao doctorDao;
    private final BCryptPasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final LoginService loginService;

    public DoctorAccountService(UserDao userDao, DoctorDao doctorDao,
                                BCryptPasswordEncoder passwordEncoder,
                                FileStorageService fileStorageService, LoginService loginService) {
        this.userDao = userDao;
        this.doctorDao = doctorDao;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.loginService = loginService;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginUser updateAvatar(Long userId, Long doctorId, String path) {
        if (!fileStorageService.isOwnedAvatarPath(userId, path)) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "头像路径无效，请先上传头像文件");
        }
        UpdateWrapper<SysUser> userUpdate = new UpdateWrapper<>();
        userUpdate.eq("id", userId).eq("deleted", 0).eq("status", 1).eq("role", "DOCTOR")
                .set("avatar", path);
        if (userDao.update(null, userUpdate) != 1) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED, "医生账号不存在或不可用");
        }
        UpdateWrapper<Doctor> doctorUpdate = new UpdateWrapper<>();
        doctorUpdate.eq("id", doctorId).eq("status", 1).set("avatar", path);
        if (doctorDao.update(null, doctorUpdate) != 1) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "医生信息不存在或已停用");
        }
        return loginService.currentUser(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, DoctorPasswordUpdateRequest request) {
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "新密码最多支持 72 个 UTF-8 字节");
        }
        SysUser user = userDao.selectById(userId);
        if (user == null || !Integer.valueOf(0).equals(user.getDeleted())
                || !Integer.valueOf(1).equals(user.getStatus()) || !"DOCTOR".equals(user.getRole())) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED, "医生账号不存在或不可用");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "当前密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "新密码不能与当前密码相同");
        }
        UpdateWrapper<SysUser> update = new UpdateWrapper<>();
        update.eq("id", userId).eq("deleted", 0).eq("status", 1).eq("role", "DOCTOR")
                .set("password", passwordEncoder.encode(request.newPassword()));
        if (userDao.update(null, update) != 1) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED, "医生账号不存在或不可用");
        }
    }
}
