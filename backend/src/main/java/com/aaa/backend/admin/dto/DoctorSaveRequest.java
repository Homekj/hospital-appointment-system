package com.aaa.backend.admin.dto;

import com.aaa.backend.entity.Doctor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * api.md 12.3 医生新增 / 修改请求体。
 *
 * <p>不暴露 doctorId 之外的服务端字段；gender 按 5.3 固定为 1(男) / 2(女)，
 * 不允许客户端写入 createTime / updateTime / 账号密码。
 */
@Data
public class DoctorSaveRequest {

    @NotNull(message = "departmentId 不能为空")
    private Long departmentId;

    @NotBlank(message = "医生姓名不能为空")
    @Size(max = 50, message = "医生姓名不能超过 50 个字符")
    private String name;

    @Size(max = 50, message = "职称不能超过 50 个字符")
    private String title;

    @Min(value = 1, message = "gender 只能是 1(男) 或 2(女)")
    @Max(value = 2, message = "gender 只能是 1(男) 或 2(女)")
    private Integer gender;

    @Size(max = 255, message = "头像路径不能超过 255 个字符")
    private String avatar;

    @Size(max = 1000, message = "医生简介不能超过 1000 个字符")
    private String intro;

    @Min(value = 0, message = "status 只能是 0(停用) 或 1(启用)")
    @Max(value = 1, message = "status 只能是 0(停用) 或 1(启用)")
    private Integer status;

    public Doctor toEntity() {
        Doctor entity = new Doctor();
        entity.setDepartmentId(departmentId);
        entity.setName(name);
        entity.setTitle(title);
        entity.setGender(gender);
        entity.setAvatar(avatar);
        entity.setIntro(intro);
        entity.setStatus(status);
        return entity;
    }
}
