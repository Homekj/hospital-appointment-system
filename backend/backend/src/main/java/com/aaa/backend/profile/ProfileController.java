package com.aaa.backend.profile;

import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.profile.dto.AvatarUpdateRequest;
import com.aaa.backend.profile.dto.ProfileUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final CurrentUserContext currentUserContext;

    public ProfileController(
            ProfileService profileService,
            CurrentUserContext currentUserContext
    ) {
        this.profileService = profileService;
        this.currentUserContext = currentUserContext;
    }

    @PutMapping
    public ApiResponse<LoginUser> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(
                profileService.updateProfile(userId, request)
        );
    }

    @PutMapping("/avatar")
    public ApiResponse<LoginUser> updateAvatar(
            @Valid @RequestBody AvatarUpdateRequest request
    ) {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(
                profileService.updateAvatar(userId, request.path())
        );
    }
}