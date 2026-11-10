package com.aaa.backend.controller;

import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.auth.JwtBlacklistService;
import com.aaa.backend.auth.dto.LoginRequest;
import com.aaa.backend.auth.dto.LoginResponse;
import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
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