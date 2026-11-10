package com.aaa.backend.controller;

import com.aaa.backend.auth.dto.LoginRequest;
import com.aaa.backend.auth.dto.LoginResponse;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class LoginController {
    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(loginService.login(request.username(), request.password()));
    }
}
