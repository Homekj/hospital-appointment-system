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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class LoginServiceimpl implements LoginService {
    private static final String MOCK_WX_OPENID = "mock-wx-login-project-demo";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_DOCTOR = "DOCTOR";
    private static final String ROLE_USER = "USER";
    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_NOT_DELETED = 0;
    private static final Logger log = LoggerFactory.getLogger(LoginServiceimpl.class);

    private final UserDao userDao;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RestClient restClient = RestClient.create();

    @Value("${wechat.miniprogram.mock-login:false}")
    private boolean mockWxLoginEnabled;

    @Value("${wechat.miniprogram.app-id:}")
    private String miniProgramAppId;

    @Value("${wechat.miniprogram.secret:}")
    private String miniProgramSecret;

    public LoginServiceimpl(UserDao userDao, BCryptPasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginUser currentUser(Long userId) {
        SysUser user = userDao.selectById(userId);

        if (user == null
                || !Integer.valueOf(STATUS_NOT_DELETED).equals(user.getDeleted())
                || !Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            throw new BusinessException(
                    BusinessErrorCode.UNAUTHORIZED,
                    "登录状态已失效，请重新登录"
            );
        }

        String role = normalizeRole(user.getRole());

        if (ROLE_DOCTOR.equals(role) && user.getDoctorId() == null) {
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
        if (!Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "账号已禁用");
        }

        String role = normalizeRole(user.getRole());
        if ("DOCTOR".equals(role) && user.getDoctorId() == null) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "医生账号未绑定医生信息");
        }

        return createLoginResponse(user, role);
    }

    @Override
    public LoginResponse wxLogin(String code, String nickname) {
        if (mockWxLoginEnabled) {
            SysUser demoUser = findByOpenId(MOCK_WX_OPENID);
            if (demoUser == null) {
                demoUser = createWxUser(MOCK_WX_OPENID, "演示患者");
            }
            return loginAsWxUser(demoUser);
        }

        if (miniProgramAppId == null || miniProgramAppId.isBlank()
                || miniProgramSecret == null || miniProgramSecret.isBlank()) {
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "微信登录暂不可用");
        }

        URI uri = UriComponentsBuilder.fromUriString("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", miniProgramAppId)
                .queryParam("secret", miniProgramSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .encode()
                .toUri();

        WxCodeSessionResponse session;
        try {
            session = restClient.get().uri(uri).retrieve().body(WxCodeSessionResponse.class);
        } catch (RestClientException exception) {
            // The exception message can contain the request URI, including the secret and login code.
            log.error("WeChat code2Session request failed, exceptionType={}",
                    exception.getClass().getSimpleName());
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "微信登录暂不可用");
        }
        Integer errcode = session == null ? null : session.errcode();
        if (session == null || (errcode != null && errcode != 0) || session.openid() == null
                || session.openid().isBlank()) {
            log.warn("WeChat login rejected, errcode={}", errcode);
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED, "微信登录凭证无效");
        }

        SysUser user = findByOpenId(session.openid());
        if (user == null) {
            user = createWxUser(session.openid(), nickname);
        }
        return loginAsWxUser(user);
    }

    private LoginResponse loginAsWxUser(SysUser user) {
        if (!Integer.valueOf(STATUS_NOT_DELETED).equals(user.getDeleted())) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "账号已停用");
        }
        if (!Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "账号已禁用");
        }
        String role = normalizeRole(user.getRole());
        if (!ROLE_USER.equals(role)) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "该微信账号不能用于患者登录");
        }
        return createLoginResponse(user, role);
    }

    private SysUser findByOpenId(String openid) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("openid", openid);
        return userDao.selectOne(query);
    }

    private SysUser createWxUser(String openid, String nickname) {
        SysUser user = new SysUser();
        user.setUsername("wx_" + UUID.randomUUID().toString().replace("-", ""));
        String randomPassword = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        user.setPassword(passwordEncoder.encode(randomPassword));
        user.setNickname(nickname == null || nickname.isBlank() ? "微信用户" : nickname.trim());
        user.setOpenid(openid);
        user.setBalance(BigDecimal.ZERO);
        user.setStatus(STATUS_ENABLED);
        user.setRole(ROLE_USER);
        user.setDeleted(STATUS_NOT_DELETED);
        try {
            userDao.insert(user);
            return user;
        } catch (DuplicateKeyException exception) {
            SysUser concurrentUser = findByOpenId(openid);
            if (concurrentUser != null) {
                return concurrentUser;
            }
            throw exception;
        }
    }

    private LoginResponse createLoginResponse(SysUser user, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("userId", user.getId());
        claims.put("role", role);
        String token = JwtUtil.createToken(claims);
        LoginUser loginUser = new LoginUser(
                user.getId(), user.getUsername(), user.getNickname(), user.getEmail(), user.getPhone(),
                user.getAvatar(), user.getBalance(), role, user.getDoctorId(), user.getStatus()
        );
        return new LoginResponse(token, loginUser);
    }

    private String normalizeRole(String databaseRole) {
        if (databaseRole == null || databaseRole.isBlank()) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "账号角色未配置");
        }
        return switch (databaseRole.trim().toUpperCase(Locale.ROOT)) {
            case ROLE_ADMIN -> ROLE_ADMIN;
            case ROLE_DOCTOR -> ROLE_DOCTOR;
            case ROLE_USER, "PATIENT" -> ROLE_USER;
            default -> throw new BusinessException(BusinessErrorCode.FORBIDDEN, "账号角色未配置");
        };
    }

    private record WxCodeSessionResponse(String openid, String session_key, Integer errcode, String errmsg) {
    }
}
