package com.aaa.backend.auth;

import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CurrentUserContext {
    private final ThreadLocal<LoginUser> holder = new ThreadLocal<>();
    private final ThreadLocal<String> tokenIdHolder = new ThreadLocal<>();
    private final ThreadLocal<Instant> expiresAtHolder = new ThreadLocal<>();
    public void set(LoginUser user,String tokenId,Instant expiresAt) {
        holder.set(user);
        tokenIdHolder.set(tokenId);
        expiresAtHolder.set(expiresAt);
    }

    public String requireTokenId() {
        String tokenId = tokenIdHolder.get();
        if (tokenId == null|| tokenId.isBlank()) {
            throw new BusinessException(
                    BusinessErrorCode.UNAUTHORIZED,
                    "当前登录令牌无效"
            );
        }
        return tokenId;
    }
    public Instant requireTokenExpiresAt() {
        Instant expireAt = expiresAtHolder.get();
        if(expireAt == null) {
            throw new BusinessException(
                    BusinessErrorCode.UNAUTHORIZED,
                    "当前登录令牌无效"
            );
        }
        return expireAt;
    }

    public LoginUser requireUser() {
        LoginUser user = holder.get();
        if (user == null) {
            throw new BusinessException(
                    BusinessErrorCode.UNAUTHORIZED,
                    "请先登录"
            );
        }
        return user;
    }

    public Long requireUserId() {
        return requireUser().id();
    }

    public Long requireDoctorId() {
        LoginUser user = requireUser();
        if (!"DOCTOR".equals(user.role()) || user.doctorId() == null) {
            throw new BusinessException(
                    BusinessErrorCode.FORBIDDEN,
                    "当前账号未绑定医生信息"
            );
        }
        return user.doctorId();
    }

    public void clear() {
        holder.remove();
        tokenIdHolder.remove();
        expiresAtHolder.remove();
    }
}