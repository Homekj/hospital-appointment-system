package com.aaa.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtBlacklistService {
    private static final Logger log =
            LoggerFactory.getLogger(JwtBlacklistService.class);

    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public JwtBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(
                    redisTemplate.hasKey(KEY_PREFIX + tokenId)
            );
        } catch (DataAccessException e) {
            log.warn("Redis unavailable; JWT blacklist check was skipped");
            return false;
        }
    }

    public void revoke(String tokenId, Instant expiresAt) {
        if (tokenId == null || tokenId.isBlank() || expiresAt == null) {
            return;
        }

        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + tokenId,
                    "1",
                    ttl
            );
        } catch (DataAccessException e) {
            log.warn("Redis unavailable; logout could not revoke JWT immediately");
        }
    }
}