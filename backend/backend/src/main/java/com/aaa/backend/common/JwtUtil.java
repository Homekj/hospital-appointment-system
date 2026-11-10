package com.aaa.backend.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    // JWT 签名密钥 (注意：至少 32 个字符，实际项目中建议放在 application.properties 中)
    private static final String JWT_SECRET = "HospitalSystemAdminSecretKey2024ForLogin";

    // 根据密钥生成 SecretKey
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

    // Token 过期时间（这里设置 24 小时，单位毫秒）
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000L;

    /**
     * 生成 Token
     * @param claims 存入 Token 的数据 (比如 userId, role 等)
     * @return 字符串 Token
     */
    public static String createToken(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims) // 设置自定义数据
                .issuedAt(new Date()) // 签发时间
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 过期时间
                .signWith(SECRET_KEY) // 签名算法
                .compact();
    }

    /**
     * 解析 Token (后续拦截器里校验登录状态时会用到)
     * @param token 前端传来的 Token
     * @return 解析后的数据载荷
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}