package com.aaa.backend.appointment.controller;

import com.aaa.backend.appointment.service.PatientProfileService;
import com.aaa.backend.appointment.service.PatientProfileService.Profile;
import com.aaa.backend.appointment.service.PatientProfileService.SaveRequest;
import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments/patients")
public class PatientProfileController {
    private final PatientProfileService profiles;
    private final CurrentUserContext currentUser;

    public PatientProfileController(PatientProfileService profiles, CurrentUserContext currentUser) {
        this.profiles = profiles;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiResponse<List<Profile>> list() {
        return ApiResponse.success(profiles.list(currentUser.requireUserId()));
    }

    @PostMapping
    public ApiResponse<List<Profile>> add(@Valid @RequestBody SaveRequest request) {
        return ApiResponse.success(profiles.save(currentUser.requireUserId(), null, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<List<Profile>> update(@PathVariable Long id, @Valid @RequestBody SaveRequest request) {
        return ApiResponse.success(profiles.save(currentUser.requireUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<List<Profile>> remove(@PathVariable Long id) {
        return ApiResponse.success(profiles.remove(currentUser.requireUserId(), id));
    }
}
