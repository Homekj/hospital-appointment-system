package com.aaa.backend.service;


import com.aaa.backend.auth.dto.LoginResponse;
import com.aaa.backend.auth.dto.LoginUser;

public interface LoginService {
    LoginResponse login(String username, String password);
    LoginUser currentUser(Long userId);
}
