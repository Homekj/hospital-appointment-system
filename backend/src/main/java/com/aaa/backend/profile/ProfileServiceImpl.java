package com.aaa.backend.profile;

import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.dao.UserDao;
import com.aaa.backend.entity.SysUser;
import com.aaa.backend.file.FileStorageService;
import com.aaa.backend.profile.dto.ProfileUpdateRequest;
import com.aaa.backend.profile.dto.PasswordUpdateRequest;
import com.aaa.backend.profile.dto.PasswordChangeRequest;
import com.aaa.backend.service.LoginService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class ProfileServiceImpl implements ProfileService {
    private final UserDao userDao;
    private final LoginService loginService;
    private final FileStorageService fileStorageService;
    private final BCryptPasswordEncoder passwordEncoder;

    public ProfileServiceImpl(
            UserDao userDao,
            LoginService loginService,
            FileStorageService fileStorageService,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.userDao = userDao;
        this.loginService = loginService;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginUser updateProfile(Long userId, ProfileUpdateRequest request) {
        String nickname = request.nickname().trim();
        String email = blankToNull(request.email());
        String phone = blankToNull(request.phone());

        UpdateWrapper<SysUser> update = new UpdateWrapper<>();
        update.eq("id", userId)
                .eq("deleted", 0)
                .eq("status", 1)
                .set("nickname", nickname)
                .set("email", email)
                .set("phone", phone);

        int rows = userDao.update(null, update);
        if (rows != 1) {
            throw new BusinessException(
                    BusinessErrorCode.UNAUTHORIZED,
                    "用户不存在或账号不可用"
            );
        }

        return loginService.currentUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginUser updateAvatar(Long userId, String path) {
        if (!fileStorageService.isOwnedAvatarPath(userId, path)) {
            throw new BusinessException(
                    BusinessErrorCode.BAD_REQUEST,
                    "头像路径无效，请先上传头像文件"
            );
        }

        UpdateWrapper<SysUser> update = new UpdateWrapper<>();
        update.eq("id", userId)
                .eq("deleted", 0)
                .eq("status", 1)
                .set("avatar", path);

        int rows = userDao.update(null, update);
        if (rows != 1) {
            throw new BusinessException(
                    BusinessErrorCode.UNAUTHORIZED,
                    "用户不存在或账号不可用"
            );
        }

        return loginService.currentUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(LoginUser currentUser, PasswordUpdateRequest request) {
        if (!"USER".equals(currentUser.role())) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN,
                    "仅患者账号可在此设置网页登录密码");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST,
                    "网页登录密码最多支持 72 个 UTF-8 字节");
        }

        UpdateWrapper<SysUser> update = new UpdateWrapper<>();
        update.eq("id", currentUser.id())
                .eq("deleted", 0)
                .eq("status", 1)
                .eq("role", "USER")
                .set("password", passwordEncoder.encode(request.password()));
        if (userDao.update(null, update) != 1) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED,
                    "用户不存在或账号不可用");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(LoginUser currentUser, PasswordChangeRequest request) {
        if (!"USER".equals(currentUser.role())) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "仅患者账号可在患者中心修改密码");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "新密码最多支持 72 个 UTF-8 字节");
        }
        SysUser user = userDao.selectById(currentUser.id());
        if (user == null || !Integer.valueOf(0).equals(user.getDeleted())
                || !Integer.valueOf(1).equals(user.getStatus()) || !"USER".equals(user.getRole())) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED, "用户不存在或账号不可用");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "当前密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "新密码不能与当前密码相同");
        }
        UpdateWrapper<SysUser> update = new UpdateWrapper<>();
        update.eq("id", currentUser.id()).eq("deleted", 0).eq("status", 1).eq("role", "USER")
                .set("password", passwordEncoder.encode(request.newPassword()));
        if (userDao.update(null, update) != 1) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED, "用户不存在或账号不可用");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
