package com.aaa.backend.auth.dto;

public record LoginResponse(String token, LoginUser user) {
}
