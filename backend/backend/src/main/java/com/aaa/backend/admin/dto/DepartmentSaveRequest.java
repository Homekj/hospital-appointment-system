package com.aaa.backend.admin.dto;

import com.aaa.backend.entity.Department;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * api.md 12.2 科室新增 / 修改请求体。
 *
 * <p>刻意不直接把 Department 实体暴露给 /api/admin/**：实体里含 createTime、
 * updateTime 等服务端字段，文档契约不允许客户端提交。这里做一次「契约 DTO → 实体」转换，
 * 长度约束与 sql/system.sql 的列定义保持一致。
 */
@Data
public class DepartmentSaveRequest {

    @NotBlank(message = "科室名称不能为空")
    @Size(max = 100, message = "科室名称不能超过 100 个字符")
    private String name;

    @Size(max = 500, message = "科室简介不能超过 500 个字符")
    private String description;

    @Size(max = 200, message = "科室位置不能超过 200 个字符")
    private String location;

    private Integer sortOrder;

    @Min(value = 0, message = "status 只能是 0(停用) 或 1(启用)")
    @Max(value = 1, message = "status 只能是 0(停用) 或 1(启用)")
    private Integer status;

    public Department toEntity() {
        Department entity = new Department();
        entity.setName(name);
        entity.setDescription(description);
        entity.setLocation(location);
        entity.setSortOrder(sortOrder);
        entity.setStatus(status);
        return entity;
    }
}
