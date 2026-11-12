package com.aaa.backend.controller;

import com.aaa.backend.common.Result;
import com.aaa.backend.dto.ScheduleBatchDTO;
import com.aaa.backend.dto.ScheduleSaveDTO;
import com.aaa.backend.service.ScheduleService;
import com.aaa.backend.vo.ScheduleQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping("/page")
    public Result page(ScheduleQuery query) {
        return scheduleService.pageQuery(query);
    }
    @PostMapping("/add")
    public Result add(@RequestBody ScheduleSaveDTO dto) {
        return scheduleService.addSchedule(dto);
    }
    @PutMapping("/update")
    public Result update(@RequestBody ScheduleSaveDTO dto) {
        return scheduleService.update(dto);
    }
    /** 同时兼容 /delete/{id} 与 /delete?id=，与科室、医生模块的风格保持一致 */
    @DeleteMapping({"/delete/{id}", "/delete"})
    public Result delete(@PathVariable(name = "id", required = false) Long pathId,
                         @RequestParam(name = "id", required = false) Long queryId) {
        return scheduleService.delete(pathId != null ? pathId : queryId);
    }
    @PutMapping("/status")
    public Result changeStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        return scheduleService.changeStatus(id, status);
    }
    @PostMapping("/batch")
    public Result batchAdd(@RequestBody ScheduleBatchDTO dto) {
        return scheduleService.batchAdd(dto);
    }

}