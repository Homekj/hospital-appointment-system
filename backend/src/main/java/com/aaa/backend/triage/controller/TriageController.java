package com.aaa.backend.triage.controller;

import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.triage.dto.TriageRequest;
import com.aaa.backend.triage.service.TriageService;
import com.aaa.backend.triage.vo.TriageResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 智能分诊为公开接口（docs/api.md 7.5 权限：公开），无需登录。 */
@RestController
@RequestMapping("/api/triage")
@CrossOrigin
public class TriageController {

    private final TriageService triageService;

    public TriageController(TriageService triageService) {
        this.triageService = triageService;
    }

    @PostMapping("/analyze")
    public ApiResponse<TriageResultVO> analyze(@Valid @RequestBody TriageRequest request) {
        return ApiResponse.success(triageService.analyze(request.symptom()));
    }
}
