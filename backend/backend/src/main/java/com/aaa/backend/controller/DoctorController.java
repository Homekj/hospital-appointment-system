package com.aaa.backend.controller;

import com.aaa.backend.common.Result;
import com.aaa.backend.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/doctor")
public class DoctorController {
    @Autowired
    private DoctorService doctorService;
    @GetMapping("/findByLikeName")
    public Result findByLikeName(String name){
        Result result = doctorService.getByLikeName(name);
        return result;
    }

}
