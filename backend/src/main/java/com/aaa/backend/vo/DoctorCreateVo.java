package com.aaa.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 新增医生的返回结果：前端需要拿到自增 id 才能跳转详情/继续排班 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCreateVo {
    private Long id;
    private String username;
    private String defaultPassword;
    private String message;
}
