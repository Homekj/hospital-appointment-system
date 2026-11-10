package com.aaa.backend.service.serviceimpl;

import com.aaa.backend.auth.dto.LoginResponse;
import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.common.JwtUtil;
import com.aaa.backend.dao.UserDao;
import com.aaa.backend.entity.SysUser;
import com.aaa.backend.service.LoginService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class LoginServiceimpl implements LoginService {
    private final UserDao userDao;
    private final BCryptPasswordEncoder passwordEncoder;

    public LoginServiceimpl(UserDao userDao, BCryptPasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginUser currentUser(Long userId) {
        SysUser user = userDao.selectById(userId);

        if (user == null
                || !Integer.valueOf(0).equals(user.getDeleted())
                || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(
                    BusinessErrorCode.UNAUTHORIZED,
                    "登录状态已失效，请重新登录"
            );
        }

        String role = normalizeRole(user.getRole());

        if ("DOCTOR".equals(role) && user.getDoctorId() == null) {
            throw new BusinessException(
                    BusinessErrorCode.UNAUTHORIZED,
                    "医生账号未绑定医生信息"
            );

        }
        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                user.getBalance(),
                role,
                user.getDoctorId(),
                user.getStatus()
        );



    }

    @Override
    public LoginResponse login(String username, String password) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", username).eq("deleted", 0);
        SysUser user = userDao.selectOne(query);

        if (user == null || user.getPassword() == null
                || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "账号已禁用");
        }

        String role = normalizeRole(user.getRole());
        if ("DOCTOR".equals(role) && user.getDoctorId() == null) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "医生账号未绑定医生信息");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("userId", user.getId());
        claims.put("role", role);
        String token = JwtUtil.createToken(claims);

        LoginUser loginUser = new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                user.getBalance(),
                role,
                user.getDoctorId(),
                user.getStatus()
        );
        return new LoginResponse(token, loginUser);
    }

    private String normalizeRole(String databaseRole) {
        if (databaseRole == null || databaseRole.isBlank()) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "账号角色未配置");
        }
        return switch (databaseRole.trim().toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> "ADMIN";
            case "DOCTOR" -> "DOCTOR";
            case "USER", "PATIENT" -> "USER";
            default -> throw new BusinessException(BusinessErrorCode.FORBIDDEN, "账号角色未配置");
        };
    }
}
