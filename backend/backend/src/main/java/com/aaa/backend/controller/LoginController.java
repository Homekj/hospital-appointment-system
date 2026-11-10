package com.aaa.backend.controller;

import com.aaa.backend.common.Result;
import com.aaa.backend.service.LoginService;
import com.aaa.backend.vo.SysUserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class LoginController {
    @Autowired
    private LoginService loginService;
    @PostMapping("/login")
    public Result login(@RequestBody SysUserVo sysUserVo) {
        // 1. 从 VO 对象中提取账号和密码
        String username = sysUserVo.getUsername();
        String password = sysUserVo.getPassword();
        Result result = loginService.selectByUsernamePassword(username, password);
        return result;
    }
}

