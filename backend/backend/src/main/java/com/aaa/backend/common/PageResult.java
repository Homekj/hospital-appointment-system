package com.aaa.backend.common;

import java.util.List;

public record PageResult<T>(List<T> records, long total, long current, long size) {
    public PageResult {
        records = records == null ? List.of() : List.copyOf(records);
        if (total < 0 || current < 1 || size < 1) {
            throw new IllegalArgumentException("分页结果参数不合法");
        }
    }
}
