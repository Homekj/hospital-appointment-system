package com.aaa.backend.common;

public enum BusinessErrorCode {
    BAD_REQUEST(400, "请求参数错误"),
    INVALID_APPOINTMENT_STATE(400, "当前预约状态不允许此操作"),
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "无权访问此资源"),
    NOT_FOUND(404, "请求的资源不存在"),
    APPOINTMENT_NOT_FOUND(404, "预约不存在"),
    CONFLICT(409, "请求与当前数据冲突"),
    SCHEDULE_FULL(409, "该时段号源已满"),
    APPOINTMENT_CONFLICT(409, "预约状态已变化，请刷新后重试"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String defaultMessage;

    BusinessErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
