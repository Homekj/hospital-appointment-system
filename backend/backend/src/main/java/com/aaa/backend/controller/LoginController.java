package com.aaa.backend.controller;

import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.auth.JwtBlacklistService;
import com.aaa.backend.auth.dto.LoginRequest;
import com.aaa.backend.auth.dto.LoginResponse;
import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.auth.dto.WxLoginRequest;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * api.md 6.1~6.4 认证接口。
 *
 * <p>按文档 17 要求，CORS 统一由 WebMvcConfig 的明确来源白名单管理，
 * 这里刻意不加 @CrossOrigin（它默认放开任意来源，会绕过白名单）。
 * 登录失败语义由 LoginServiceimpl 抛出 BusinessException：
 * 401 用户名或密码错误 / 403 账号已禁用，经 GlobalExceptionHandler 输出
 * {code,data,message} 且 HTTP 状态码与业务码一致。
 */
@RestController
@RequestMapping("/api/auth")
public class LoginController {
    private final LoginService loginService;
    private final CurrentUserContext currentUserContext;
    private final JwtBlacklistService jwtBlacklistService;

    public LoginController(
            LoginService loginService,
            CurrentUserContext currentUserContext,
            JwtBlacklistService jwtBlacklistService
    ) {
        this.loginService = loginService;
        this.currentUserContext = currentUserContext;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ApiResponse.success(
                loginService.login(request.username(), request.password())
        );
    }

    @PostMapping("/wx-login")
    public ApiResponse<LoginResponse> wxLogin(
            @Valid @RequestBody WxLoginRequest request
    ) {
        return ApiResponse.success(
                loginService.wxLogin(request.code(), request.nickname())
        );
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        jwtBlacklistService.revoke(
                currentUserContext.requireTokenId(),
                currentUserContext.requireTokenExpiresAt()
        );
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<LoginUser> me() {
        return ApiResponse.success(currentUserContext.requireUser());
    }
}
