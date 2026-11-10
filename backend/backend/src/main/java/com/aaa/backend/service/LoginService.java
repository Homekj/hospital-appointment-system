package com.aaa.backend.service;

import com.aaa.backend.common.Result;

public interface LoginService {
    public Result selectByUsernamePassword(String username,String password);
}
