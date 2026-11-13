package com.aaa.backend.admin.support;

import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.common.PageResult;
import com.aaa.backend.common.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 旧 /admin/** 模块返回 Result（错误时 code=500 且 HTTP 200），
 * 而 api.md 3.3 要求 HTTP 状态码与业务码一致、3.1 要求统一 {code,data,message}。
 *
 * <p>本桥接器让新增的 /api/admin/** 契约接口复用旧 Service 的业务逻辑，
 * 同时输出符合文档的响应；旧接口保持原样，按文档 17.7 仅作迁移兼容。
 */
public final class ResultBridge {

    private ResultBridge() {
    }

    public static ApiResponse<Object> data(Result result) {
        if (result == null) {
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR);
        }
        if (result.code() == 200) {
            return ApiResponse.success(result.data());
        }
        throw toException(result.message());
    }

    /** 把 MyBatis-Plus 的 IPage 收敛成文档 3.2 规定的四字段分页对象 */
    @SuppressWarnings("unchecked")
    public static ApiResponse<PageResult<Object>> page(Result result) {
        if (result == null) {
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR);
        }
        if (result.code() != 200) {
            throw toException(result.message());
        }
        Object raw = result.data();
        if (raw instanceof IPage<?> page) {
            return ApiResponse.success(new PageResult<>(
                    (List<Object>) page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
        }
        throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "分页结果格式异常");
    }

    /** 按错误语义映射 HTTP 状态码与业务码，避免所有失败都被吞成 500 */
    private static BusinessException toException(String message) {
        String text = message == null || message.isBlank() ? "请求失败" : message;
        if (text.contains("不存在")) {
            return new BusinessException(BusinessErrorCode.NOT_FOUND, text);
        }
        if (text.contains("已存在") || text.contains("重复") || text.contains("已有")
                || text.contains("无法删除") || text.contains("无法停用")
                || text.contains("不能重复") || text.contains("请勿重复")) {
            return new BusinessException(BusinessErrorCode.CONFLICT, text);
        }
        return new BusinessException(BusinessErrorCode.BAD_REQUEST, text);
    }
}
