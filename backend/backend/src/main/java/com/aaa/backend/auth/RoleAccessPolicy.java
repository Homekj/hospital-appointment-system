package com.aaa.backend.auth;

import org.springframework.stereotype.Component;

@Component
public class RoleAccessPolicy {
    public boolean isAllowed(String path, String role) {
        if (path == null || role == null) {
            return false;
        }

        //管理域仅ADMIN
        if (isUnder(path,"/api/admin")){
            return "ADMIN".equals(role);
        }

        //旧后台管理接口 /admin/**（科室/医生/排班等）同样仅 ADMIN。
        //缺少这条时，一旦把 /admin/** 纳入拦截，末尾默认 return false 会导致全 403。
        if (isUnder(path,"/admin")){
            return "ADMIN".equals(role);
        }

        //医生域仅DOCTOR
        if (isUnder(path,"/api/doctor")){
            return "DOCTOR".equals(role);
        }

        //患者业务仅USER
        if(isUnder(path,"/api/appointments")
        || isUnder(path,"/api/notifications")
        || isUnder(path,"/api/wallet")
        || isUnder(path,"/api/prescriptions")){
            return "USER".equals(role);
        }
        //所有已登录角色都可使用
        if("/api/auth/me".equals(path)
            ||"/api/auth/logout".equals(path)
            || isUnder(path,"/api/profile")
            || "/api/files/upload".equals(path)){
            return "ADMIN".equals(role)
                    || "DOCTOR".equals(role)
                    || "USER".equals(role);
        }

        //没有配置权限的受保护接口默认拒绝
        return false;
    }

    private boolean isUnder(String path, String prefix){
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
