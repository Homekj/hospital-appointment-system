package com.aaa.backend.service.serviceimpl;

import com.aaa.backend.common.JwtUtil;
import com.aaa.backend.common.Result;
import com.aaa.backend.dao.UserDao;
import com.aaa.backend.entity.SysUser;
import com.aaa.backend.service.LoginService;
import com.aaa.backend.vo.SysUserVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LoginServiceimpl implements LoginService {
    @Autowired
    private UserDao loginDao;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public Result selectByUsernamePassword(String username, String password){
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username",username);
        SysUser sysUser = loginDao.selectOne(queryWrapper);
        if (sysUser == null){
            return Result.error("账号或密码错误");
        }
        boolean isMatch = passwordEncoder.matches(password, sysUser.getPassword());
        if (!isMatch) {
            return Result.error("账号或密码错误");
        }
        String role;
        String redirectUrl;
        if ("admin".equals(sysUser.getUsername())) {
            // 管理员
            role = "admin";
            redirectUrl = "/admin/dashboard"; // 后台首页路径
        } else if (sysUser.getUsername().startsWith("doctor")) {
            // 医生账号（你表里医生的账号都是 doctor 开头的）
            role = "doctor";
            redirectUrl = "/doctor/home";     // 医生工作台路径
        } else {
            // 其他视为患者
            role = "patient";
            redirectUrl = "/patient/appointment"; // 患者预约页面路径
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", sysUser.getId());
        claims.put("username", sysUser.getUsername());
        claims.put("role", role);
        String token = JwtUtil.createToken(claims);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);             // 唯一标识
        data.put("role", role);               // 角色
        data.put("redirectUrl", redirectUrl); // 前端跳转路径
        data.put("nickname", sysUser.getNickname());
        data.put("avatar", sysUser.getAvatar());

        return Result.ok(data);

    }
}
