package com.aaa.backend.profile;

import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.profile.dto.ProfileUpdateRequest;

public interface ProfileService {
    LoginUser updateProfile(Long userId, ProfileUpdateRequest request);

    LoginUser updateAvatar(Long userId, String path);
}