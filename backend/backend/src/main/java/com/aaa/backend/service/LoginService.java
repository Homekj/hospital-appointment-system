package com.aaa.backend.service;

import com.aaa.backend.auth.dto.LoginResponse;

public interface LoginService {
    LoginResponse login(String username, String password);
}
