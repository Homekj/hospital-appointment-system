package com.aaa.backend.catalog.controller;

import com.aaa.backend.catalog.service.CatalogService;
import com.aaa.backend.catalog.vo.DepartmentVO;
import com.aaa.backend.catalog.vo.DoctorVO;
import com.aaa.backend.catalog.vo.ScheduleVO;
import com.aaa.backend.common.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 公开接口：/api/departments、/api/doctors、/api/doctors/{id}、/api/schedules。 */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/departments")
    public ApiResponse<List<DepartmentVO>> departments(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(catalogService.departments(keyword));
    }

    @GetMapping("/doctors")
    public ApiResponse<List<DoctorVO>> doctors(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean hasAvailableSlot) {
        return ApiResponse.success(catalogService.doctors(departmentId, keyword, hasAvailableSlot));
    }

    @GetMapping("/doctors/{id}")
    public ApiResponse<DoctorVO> doctor(@PathVariable Long id) {
        return ApiResponse.success(catalogService.doctor(id));
    }

    @GetMapping("/schedules")
    public ApiResponse<List<ScheduleVO>> schedules(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(catalogService.schedules(doctorId, scheduleDate, startDate, endDate));
    }
}
