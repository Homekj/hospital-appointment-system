package com.aaa.backend.profile;

import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.dao.UserDao;
import com.aaa.backend.entity.SysUser;
import com.aaa.backend.file.FileStorageService;
import com.aaa.backend.profile.dto.ProfileUpdateRequest;
import com.aaa.backend.service.LoginService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileServiceImpl implements ProfileService {
    private final UserDao userDao;
    private final LoginService loginService;
    private final FileStorageService fileStorageService;

    public ProfileServiceImpl(
            UserDao userDao,
            LoginService loginService,
            FileStorageService fileStorageService
    ) {
        this.userDao = userDao;
        this.loginService = loginService;
        this.fileStorageService = fileStorageService;
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

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
