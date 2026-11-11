package com.aaa.backend.common;

/** Legacy controller response kept for source compatibility with old services. */
public record Result(int code, Object data, String message) {
    public static Result ok() {
        return new Result(200, null, "success");
    }

    public static Result ok(Object data) {
        return new Result(200, data, "success");
    }

    public static Result error() {
        return new Result(500, null, "error");
    }

    public static Result error(String message) {
        return new Result(500, null, message);
    }
}
