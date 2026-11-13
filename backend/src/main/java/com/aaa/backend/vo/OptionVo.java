package com.aaa.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下拉框选项：只暴露 id / name，
 * 避免把 Department、Doctor 实体里未查询的字段以 Java 默认值返回给前端。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionVo {
    private Long id;
    private String name;
}
